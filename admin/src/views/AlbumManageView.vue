<script setup>
import { computed, ref, watch } from 'vue'
import AdminLayout from '../modules/dashboard/components/AdminLayout.vue'
import { useAlbumAdmin } from '../modules/album/composables/useAlbumAdmin'
import AuthImage from '../modules/album/components/AuthImage.vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { fetchAlbumDetail, updateAlbum, uploadAlbumImage } from '../modules/album/api/albumApi'

const {
  loading,
  error,
  albums,
  page,
  total,
  totalPages,
  hasPrevPage,
  hasNextPage,
  form,
  uploadedImages,
  uploading,
  isEdit,
  loadAlbums,
  uploadFiles,
  editAlbum,
  removeImage,
  saveAlbum,
  removeAlbum,
  prevPage,
  nextPage,
  resetForm
} = useAlbumAdmin()

const isDragOver = ref(false)
const coverInputRef = ref(null)
const appendInputRef = ref(null)
const previewVisible = ref(false)
const previewUrl = ref('')
const descMode = ref('edit')
const selectedTemplate = ref('')
const customTemplateName = ref('')
const customTemplates = ref([])
const TEMPLATE_STORAGE_KEY = 'albumMarkdownTemplates'
const subImagePage = ref(1)
const subImagePageSize = 15
const editorVisible = ref(false)
const activeAlbumId = ref(null)
const quickAlbumLoading = ref(false)
const quickAlbumError = ref('')
const quickAlbumDetail = ref(null)
const quickSubImagePage = ref(1)
const quickSubImagePageSize = 15
const quickAppendInputRef = ref(null)
const quickUploading = ref(false)

const markdownTemplates = [
  {
    key: 'travel',
    label: '旅行日志模板',
    content: `## 行程亮点
- 地点：  
- 时间：  
- 推荐指数：⭐️⭐️⭐️⭐️⭐️

## 拍摄故事
写下这组图片背后的故事...

## 出行小贴士
1. 
2. 
3. `
  },
  {
    key: 'product',
    label: '产品展示模板',
    content: `## 核心卖点
- 卖点 1
- 卖点 2

## 使用体验
> 真实体验描述

## 参数信息
| 项目 | 内容 |
| --- | --- |
| 尺寸 |  |
| 重量 |  |`
  },
  {
    key: 'daily',
    label: '日常分享模板',
    content: `## 今日记录
一句话总结今天的主题

## 图片看点
- 
- 

## 想说的话
这里填写你的感受...`
  }
]

const allTemplates = computed(() => [...markdownTemplates, ...customTemplates.value])
const selectedTemplateKey = computed(() => (typeof selectedTemplate.value === 'string' ? selectedTemplate.value : ''))
const isCustomTemplateSelected = computed(() => selectedTemplateKey.value.startsWith('custom-'))

const coverImage = computed(() =>
  uploadedImages.value.find((img) => img.url === form.value.coverUrl) ?? null
)
const descriptionPreview = computed(() => form.value.description || '')
const toAssetUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return url.startsWith('/') ? url : `/${url}`
}
const subImageTotal = computed(() => uploadedImages.value.length)
const subImageTotalPages = computed(() => Math.max(1, Math.ceil(subImageTotal.value / subImagePageSize)))
const hasSubImagePrevPage = computed(() => subImagePage.value > 1)
const hasSubImageNextPage = computed(() => subImagePage.value < subImageTotalPages.value)
const pagedUploadedImages = computed(() => {
  const start = (subImagePage.value - 1) * subImagePageSize
  return uploadedImages.value.slice(start, start + subImagePageSize)
})
const quickSubImageTotal = computed(() => quickAlbumDetail.value?.images?.length ?? 0)
const quickSubImageTotalPages = computed(() => Math.max(1, Math.ceil(quickSubImageTotal.value / quickSubImagePageSize)))
const hasQuickSubImagePrevPage = computed(() => quickSubImagePage.value > 1)
const hasQuickSubImageNextPage = computed(() => quickSubImagePage.value < quickSubImageTotalPages.value)
const quickPagedImages = computed(() => {
  const images = quickAlbumDetail.value?.images ?? []
  const start = (quickSubImagePage.value - 1) * quickSubImagePageSize
  return images.slice(start, start + quickSubImagePageSize)
})

const loadCustomTemplates = () => {
  try {
    const raw = localStorage.getItem(TEMPLATE_STORAGE_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return
    customTemplates.value = parsed.filter(
      (item) => item && typeof item.key === 'string' && typeof item.label === 'string' && typeof item.content === 'string'
    )
  } catch {
    customTemplates.value = []
  }
}

const persistCustomTemplates = () => {
  localStorage.setItem(TEMPLATE_STORAGE_KEY, JSON.stringify(customTemplates.value))
}

const onFileChange = async (event) => {
  const files = Array.from(event.target.files ?? [])
  await uploadFiles(files)
  event.target.value = ''
}

const onDropUpload = async (event) => {
  event.preventDefault()
  isDragOver.value = false
  const files = Array.from(event.dataTransfer?.files ?? [])
  await uploadFiles(files)
}

const onDragOver = (event) => {
  event.preventDefault()
  isDragOver.value = true
}

const onDragLeave = () => {
  isDragOver.value = false
}

const openFileDialog = () => {
  coverInputRef.value?.click()
}

const openAppendDialog = () => {
  appendInputRef.value?.click()
}

const onAppendFileChange = async (event) => {
  const files = Array.from(event.target.files ?? [])
  await uploadFiles(files)
  event.target.value = ''
}

const previewImage = (url) => {
  previewUrl.value = toAssetUrl(url)
  previewVisible.value = true
}

const applyTemplate = () => {
  const selected = allTemplates.value.find((item) => item.key === selectedTemplate.value)
  if (!selected) return
  form.value.description = selected.content
  descMode.value = 'edit'
}

const saveAsCustomTemplate = () => {
  const content = (form.value.description || '').trim()
  const label = customTemplateName.value.trim()
  if (!label || !content) return
  customTemplates.value.unshift({
    key: `custom-${Date.now()}`,
    label,
    content
  })
  persistCustomTemplates()
  customTemplateName.value = ''
}

const removeSelectedTemplate = () => {
  if (!isCustomTemplateSelected.value) return
  customTemplates.value = customTemplates.value.filter((item) => item.key !== selectedTemplateKey.value)
  persistCustomTemplates()
  selectedTemplate.value = ''
}

const prevSubImagePage = () => {
  if (hasSubImagePrevPage.value) {
    subImagePage.value -= 1
  }
}

const nextSubImagePage = () => {
  if (hasSubImageNextPage.value) {
    subImagePage.value += 1
  }
}

const removeSubImage = (url) => {
  removeImage(url)
  if (subImagePage.value > subImageTotalPages.value) {
    subImagePage.value = subImageTotalPages.value
  }
}

const openCreateEditor = () => {
  resetForm()
  subImagePage.value = 1
  descMode.value = 'edit'
  editorVisible.value = true
}

const openEditEditor = async (albumId) => {
  editorVisible.value = true
  descMode.value = 'edit'
  subImagePage.value = 1
  await editAlbum(albumId)
}

const closeEditor = () => {
  editorVisible.value = false
}

const submitEditor = async () => {
  await saveAlbum()
  if (!error.value) {
    editorVisible.value = false
  }
}

const loadQuickAlbum = async (albumId) => {
  quickAlbumLoading.value = true
  quickAlbumError.value = ''
  try {
    const { data } = await fetchAlbumDetail(albumId)
    quickAlbumDetail.value = data?.data ?? null
    quickSubImagePage.value = 1
  } catch (err) {
    quickAlbumError.value = err?.message ?? '加载子图失败'
  } finally {
    quickAlbumLoading.value = false
  }
}

const onAlbumRowClick = async ({ data }) => {
  if (!data?.id) return
  activeAlbumId.value = data.id
  await loadQuickAlbum(data.id)
}

const selectQuickCover = (url) => {
  if (!quickAlbumDetail.value) return
  quickAlbumDetail.value = {
    ...quickAlbumDetail.value,
    coverUrl: url
  }
}

const saveQuickCover = async () => {
  if (!quickAlbumDetail.value) return
  quickAlbumError.value = ''
  try {
    const detail = quickAlbumDetail.value
    await updateAlbum(detail.id, {
      title: detail.title,
      description: detail.description,
      coverUrl: detail.coverUrl || '',
      imageFolder: detail.imageFolder || ''
    })
    await loadAlbums(page.value)
  } catch (err) {
    quickAlbumError.value = err?.message ?? '保存封面失败'
  }
}

const prevQuickSubImagePage = () => {
  if (hasQuickSubImagePrevPage.value) quickSubImagePage.value -= 1
}

const nextQuickSubImagePage = () => {
  if (hasQuickSubImageNextPage.value) quickSubImagePage.value += 1
}

const openQuickUploadDialog = () => {
  quickAppendInputRef.value?.click()
}

const onQuickUploadChange = async (event) => {
  const files = Array.from(event.target.files ?? [])
  event.target.value = ''
  if (!quickAlbumDetail.value || files.length === 0) return
  quickUploading.value = true
  quickAlbumError.value = ''
  try {
    for (const file of files) {
      await uploadAlbumImage(file, quickAlbumDetail.value.imageFolder)
    }
    await loadQuickAlbum(quickAlbumDetail.value.id)
    await loadAlbums(page.value)
  } catch (err) {
    quickAlbumError.value = err?.message ?? '批量上传失败'
  } finally {
    quickUploading.value = false
  }
}

watch(
  () => uploadedImages.value.length,
  () => {
    if (subImagePage.value > subImageTotalPages.value) {
      subImagePage.value = subImageTotalPages.value
    }
  }
)

loadCustomTemplates()
</script>

<template>
  <AdminLayout :loading="loading" @refresh="loadAlbums">
    <section class="content-grid album-grid">
      <article class="panel">
        <div class="panel-header">
          <h2>相册列表</h2>
          <Button label="新建相册" icon="pi pi-plus" @click="openCreateEditor" />
        </div>
        <DataTable
          :value="albums"
          data-key="id"
          striped-rows
          size="small"
          scrollable
          scroll-height="560px"
          :row-class="(rowData) => (rowData.id === activeAlbumId ? 'album-row-active' : '')"
          @row-click="onAlbumRowClick"
        >
          <Column field="id" header="ID" />
          <Column header="封面">
            <template #body="{ data }">
              <AuthImage
                v-if="data.coverUrl"
                class="album-cover-thumb"
                :src="toAssetUrl(data.coverUrl)"
                :alt="data.title || 'cover'"
              />
              <span v-else>-</span>
            </template>
          </Column>
          <Column field="title" header="标题" />
          <Column field="description" header="描述" />
          <Column field="imageCount" header="图片数" />
          <Column header="操作">
            <template #body="{ data }">
              <div class="ops">
                <Button label="编辑" text size="small" icon="pi pi-pencil" @click="openEditEditor(data.id)" />
                <Button label="删除" text severity="danger" size="small" icon="pi pi-trash" @click="removeAlbum(data.id)" />
              </div>
            </template>
          </Column>
        </DataTable>
        <div class="pagination-bar">
          <p>共 {{ total }} 条 · 第 {{ page }} / {{ totalPages }} 页</p>
          <div class="ops">
            <Button label="上一页" size="small" :disabled="!hasPrevPage" @click="prevPage" />
            <Button label="下一页" size="small" :disabled="!hasNextPage" @click="nextPage" />
          </div>
        </div>
      </article>

    </section>

    <article v-if="quickAlbumDetail || quickAlbumLoading || quickAlbumError" class="panel quick-manager-panel">
      <div class="panel-header">
        <h2>子图快速管理</h2>
        <div class="ops">
          <Button
            v-if="quickAlbumDetail"
            label="批量上传子图"
            icon="pi pi-upload"
            :loading="quickUploading"
            @click="openQuickUploadDialog"
          />
          <Button
            v-if="quickAlbumDetail"
            label="进入编辑弹窗"
            text
            icon="pi pi-external-link"
            @click="openEditEditor(quickAlbumDetail.id)"
          />
          <input
            ref="quickAppendInputRef"
            class="hidden-file-input"
            type="file"
            accept="image/*"
            multiple
            @change="onQuickUploadChange"
          />
        </div>
      </div>
      <p v-if="quickAlbumLoading">加载中...</p>
      <p v-else-if="quickAlbumError" class="error">{{ quickAlbumError }}</p>
      <template v-else-if="quickAlbumDetail">
        <p>当前相册：{{ quickAlbumDetail.title }}（{{ quickSubImageTotal }} 张）</p>
        <div class="paged-grid-body">
          <ul class="image-grid">
            <li v-for="img in quickPagedImages" :key="img.id || img.url" class="image-card">
              <span v-if="quickAlbumDetail.coverUrl === img.url" class="cover-badge">封面</span>
              <AuthImage class="thumb" :src="toAssetUrl(img.url)" :alt="img.originalName || img.url" @click="previewImage(img.url)" />
              <div class="image-actions">
                <Button label="预览" text size="small" @click="previewImage(img.url)" />
                <Button
                  :label="quickAlbumDetail.coverUrl === img.url ? '当前封面' : '设为封面'"
                  text
                  size="small"
                  @click="selectQuickCover(img.url)"
                />
              </div>
            </li>
          </ul>
        </div>
        <div class="pagination-bar">
          <p>子图 {{ quickSubImageTotal }} 张 · 第 {{ quickSubImagePage }} / {{ quickSubImageTotalPages }} 页</p>
          <div class="ops">
            <Button label="上一页" size="small" :disabled="!hasQuickSubImagePrevPage" @click="prevQuickSubImagePage" />
            <Button label="下一页" size="small" :disabled="!hasQuickSubImageNextPage" @click="nextQuickSubImagePage" />
            <Button label="保存封面设置" icon="pi pi-check" @click="saveQuickCover" />
          </div>
        </div>
      </template>
    </article>

    <Dialog
      v-model:visible="editorVisible"
      modal
      :draggable="false"
      :style="{ width: 'min(1100px, 94vw)' }"
    >
      <template #header>
        <div class="panel-header" style="width: 100%;">
          <h2>{{ isEdit ? '编辑相册' : '新建相册' }}</h2>
          <Button label="关闭" text icon="pi pi-times" @click="closeEditor" />
        </div>
      </template>
      <p v-if="error" class="error">{{ error }}</p>
      <div class="album-form">
          <label>
            标题
            <InputText v-model="form.title" placeholder="请输入相册标题" />
          </label>
          <label>
            描述
            <div class="markdown-template-bar">
              <Select
                v-model="selectedTemplate"
                :options="allTemplates"
                option-label="label"
                option-value="key"
                placeholder="选择 Markdown 模板"
                class="w-full"
              />
              <Button label="应用模板" @click="applyTemplate" />
              <Button
                label="删除自定义"
                severity="danger"
                :disabled="!isCustomTemplateSelected"
                @click="removeSelectedTemplate"
              />
            </div>
            <div class="markdown-template-bar">
              <InputText
                v-model="customTemplateName"
                placeholder="输入自定义模板名称"
              />
              <Button label="保存当前内容为模板" @click="saveAsCustomTemplate" />
            </div>
            <div class="markdown-mode-tabs">
              <Button
                label="编辑"
                :class="{ active: descMode === 'edit' }"
                @click="descMode = 'edit'"
              />
              <Button
                label="预览"
                :class="{ active: descMode === 'preview' }"
                @click="descMode = 'preview'"
              />
            </div>
            <Textarea
              v-if="descMode === 'edit'"
              v-model="form.description"
              rows="6"
              placeholder="请输入 Markdown 描述"
            />
            <article
              v-else
              class="markdown-preview markdown-plain-preview"
            >{{ descriptionPreview }}</article>
          </label>
          <label>
            图片目录
            <InputText v-model="form.imageFolder" placeholder="例如：album-20260507" />
          </label>
          <label>上传图片（支持去重）</label>
          <div
            class="cover-dropzone"
            :class="{ active: isDragOver }"
            @drop="onDropUpload"
            @dragover="onDragOver"
            @dragleave="onDragLeave"
            @click="openFileDialog"
          >
            <p>拖拽相册封面/图片到这里，或点击选择文件</p>
            <input
              ref="coverInputRef"
              class="hidden-file-input"
              type="file"
              accept="image/*"
              multiple
              @change="onFileChange"
            />
          </div>
          <p v-if="uploading">上传中...</p>
          <div class="cover-preview" v-if="coverImage">
            <p>封面预览</p>
            <AuthImage :src="toAssetUrl(coverImage.url)" :alt="coverImage.originalName || 'cover'" />
          </div>

          <div class="sub-image-header">
            <p>子图片管理</p>
            <Button label="继续上传子图片" icon="pi pi-upload" @click="openAppendDialog" />
            <input
              ref="appendInputRef"
              class="hidden-file-input"
              type="file"
              accept="image/*"
              multiple
              @change="onAppendFileChange"
            />
          </div>

          <div class="paged-grid-body">
            <ul class="image-grid">
              <li v-for="img in pagedUploadedImages" :key="img.id" class="image-card">
                <span v-if="form.coverUrl === img.url" class="cover-badge">封面</span>
                <AuthImage
                  class="thumb"
                  :src="toAssetUrl(img.url)"
                  :alt="img.originalName || img.url"
                  @click="previewImage(img.url)"
                />
                <div class="image-actions">
                  <Button label="预览" text size="small" @click="previewImage(img.url)" />
                  <Button
                    :label="form.coverUrl === img.url ? '当前封面' : '设为封面'"
                    text
                    size="small"
                    @click="form.coverUrl = img.url"
                  />
                  <Button label="删除" text severity="danger" size="small" @click="removeSubImage(img.url)" />
                </div>
              </li>
            </ul>
          </div>
          <div class="pagination-bar">
            <p>子图 {{ subImageTotal }} 张 · 第 {{ subImagePage }} / {{ subImageTotalPages }} 页</p>
            <div class="ops">
              <Button label="上一页" size="small" :disabled="!hasSubImagePrevPage" @click="prevSubImagePage" />
              <Button label="下一页" size="small" :disabled="!hasSubImageNextPage" @click="nextSubImagePage" />
            </div>
          </div>
          <div class="ops">
            <Button :label="isEdit ? '更新相册' : '创建相册'" icon="pi pi-check" @click="submitEditor" />
            <Button label="重置" text icon="pi pi-refresh" @click="resetForm" />
          </div>
      </div>
    </Dialog>

    <section v-if="previewVisible" class="image-preview-mask" @click.self="previewVisible = false">
      <div class="image-preview-panel">
        <AuthImage :src="previewUrl" alt="preview" />
      </div>
    </section>
  </AdminLayout>
</template>

<style scoped>
.album-grid {
  grid-template-columns: minmax(0, 1fr) !important;
}

.album-cover-thumb {
  width: 72px;
  height: 48px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #d4dde8;
}

:deep(.album-row-active) {
  background: rgba(59, 130, 246, 0.08);
}

.quick-manager-panel {
  margin-top: 14px;
}

.quick-manager-panel .image-grid {
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.paged-grid-body {
  height: 430px;
  overflow: auto;
}

.quick-manager-panel .image-card {
  padding: 6px;
  gap: 6px;
}

.quick-manager-panel .thumb {
  height: 92px;
}

@media (max-width: 1600px) {
  .quick-manager-panel .image-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }
}

@media (max-width: 1400px) {
  .quick-manager-panel .image-grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}
</style>
