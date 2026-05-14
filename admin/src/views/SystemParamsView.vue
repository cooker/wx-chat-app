<script setup>
import { computed, onMounted, ref } from 'vue'
import AdminLayout from '../modules/dashboard/components/AdminLayout.vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import {
  fetchImageCdnSetting,
  saveImageCdnSetting,
  fetchFeedPageSetting,
  saveFeedPageSetting,
  fetchHotAlbumSizeSetting,
  saveHotAlbumSizeSetting,
  fetchSiteHeaderSetting,
  saveSiteHeaderSetting
} from '../api/systemSettingsApi'
import { getImageCdnBase, setImageCdnBase } from '../utils/assetUrl'

const mode = ref('local')
const cdnDomain = ref('')
const feedPageSize = ref(8)
const hotAlbumSize = ref(8)
const siteTopTitle = ref('')
const siteTopDescription = ref('')
const siteHeaderScript = ref('')

const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const saveError = ref('')
const saveOkHint = ref('')

const examplePath = '/uploads/albums/demo/cover.webp'

function draftCdnBase() {
  if (mode.value !== 'cdn') return ''
  const draft = cdnDomain.value.trim()
  if (!draft) return ''
  const schemeSep = draft.indexOf('://')
  let b = draft
  if (schemeSep > 0) {
    const scheme = draft.slice(0, schemeSep).toLowerCase()
    if (scheme !== 'http' && scheme !== 'https') {
      return ''
    }
  } else {
    b = `https://${draft}`
  }
  while (b.endsWith('/')) {
    b = b.slice(0, -1)
  }
  return b
}

const previewUrl = computed(() => {
  if (mode.value === 'local') {
    return examplePath
  }
  const base = draftCdnBase()
  if (!base) {
    return '（请先填写 CDN 域名）'
  }
  return `${base}${examplePath}`
})

function clearSaveFeedback() {
  saveError.value = ''
  saveOkHint.value = ''
}

onMounted(async () => {
  loading.value = true
  loadError.value = ''
  try {
    const [cdn, feed, hot, siteHeader] = await Promise.all([
      fetchImageCdnSetting(),
      fetchFeedPageSetting(),
      fetchHotAlbumSizeSetting(),
      fetchSiteHeaderSetting()
    ])
    const base = typeof cdn.imageCdnBase === 'string' ? cdn.imageCdnBase : ''
    setImageCdnBase(base)
    if (base) {
      mode.value = 'cdn'
      cdnDomain.value = base
    } else {
      mode.value = 'local'
      cdnDomain.value = ''
    }
    const fps = feed?.feedPageSize
    const n = typeof fps === 'number' ? fps : parseInt(String(fps ?? '8'), 10)
    feedPageSize.value = Number.isFinite(n) ? Math.min(100, Math.max(1, n)) : 8
    const has = hot?.hotAlbumSize
    const h = typeof has === 'number' ? has : parseInt(String(has ?? '8'), 10)
    hotAlbumSize.value = Number.isFinite(h) ? Math.min(30, Math.max(1, h)) : 8
    siteTopTitle.value = typeof siteHeader?.topTitle === 'string' ? siteHeader.topTitle : ''
    siteTopDescription.value = typeof siteHeader?.topDescription === 'string' ? siteHeader.topDescription : ''
    siteHeaderScript.value = typeof siteHeader?.headerScript === 'string' ? siteHeader.headerScript : ''
  } catch (e) {
    loadError.value = e?.response?.data?.message || e?.message || '加载配置失败'
  } finally {
    loading.value = false
  }
})

const onModeChange = (next) => {
  mode.value = next
  clearSaveFeedback()
  if (next === 'local') {
    cdnDomain.value = ''
  } else if (!cdnDomain.value.trim() && getImageCdnBase()) {
    cdnDomain.value = getImageCdnBase()
  }
}

function errMsg(e) {
  return e?.response?.data?.message || e?.message || '保存失败'
}

function validateBeforeSave() {
  const errs = []
  if (mode.value === 'cdn') {
    const d = cdnDomain.value.trim()
    if (!d) {
      errs.push('已选择 CDN 时请填写域名或根 URL')
    } else if (!draftCdnBase()) {
      errs.push('CDN 域名格式无效（仅支持 http/https）')
    }
  }
  const feedRaw = parseInt(String(feedPageSize.value).trim(), 10)
  if (!Number.isFinite(feedRaw) || feedRaw < 1 || feedRaw > 100) {
    errs.push('访客分页：请输入 1～100 之间的整数')
  }
  const hotRaw = parseInt(String(hotAlbumSize.value).trim(), 10)
  if (!Number.isFinite(hotRaw) || hotRaw < 1 || hotRaw > 30) {
    errs.push('热门相册：请输入 1～30 之间的整数')
  }
  return errs
}

const onSaveAll = async () => {
  clearSaveFeedback()
  const validationErrs = validateBeforeSave()
  if (validationErrs.length > 0) {
    saveError.value = validationErrs.join('；')
    return
  }

  const feedRaw = parseInt(String(feedPageSize.value).trim(), 10)
  const hotRaw = parseInt(String(hotAlbumSize.value).trim(), 10)
  const cdnPayload = mode.value === 'local' ? '' : cdnDomain.value.trim()

  saving.value = true
  try {
    await saveImageCdnSetting(cdnPayload)

    const feedOut = await saveFeedPageSetting(feedRaw)
    const storedFeed = feedOut?.feedPageSize
    if (typeof storedFeed === 'number') {
      feedPageSize.value = storedFeed
    }

    const hotOut = await saveHotAlbumSizeSetting(hotRaw)
    const storedHot = hotOut?.hotAlbumSize
    if (typeof storedHot === 'number') {
      hotAlbumSize.value = storedHot
    }

    const siteOut = await saveSiteHeaderSetting({
      topTitle: siteTopTitle.value,
      topDescription: siteTopDescription.value,
      headerScript: siteHeaderScript.value
    })
    if (typeof siteOut?.topTitle === 'string') siteTopTitle.value = siteOut.topTitle
    if (typeof siteOut?.topDescription === 'string') siteTopDescription.value = siteOut.topDescription
    if (typeof siteOut?.headerScript === 'string') siteHeaderScript.value = siteOut.headerScript

    saveOkHint.value = '已全部保存'
  } catch (e) {
    saveError.value = errMsg(e)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <AdminLayout :loading="loading">
    <article class="panel full">
      <h2>系统参数配置</h2>
      <p v-if="loadError" class="err">{{ loadError }}</p>

      <section class="block">
        <h3>图片 CDN</h3>
        <p class="hint">
          图片资源地址：选择本地（与接口同源）或由 CDN 域名拼接 <code>/uploads/...</code> 路径。
        </p>
        <div class="mode-row">
          <label class="mode-option">
            <input type="radio" name="img-mode" value="local" :checked="mode === 'local'" @change="onModeChange('local')" />
            <span>本地（同源路径）</span>
          </label>
          <label class="mode-option">
            <input type="radio" name="img-mode" value="cdn" :checked="mode === 'cdn'" @change="onModeChange('cdn')" />
            <span>CDN</span>
          </label>
        </div>

        <div v-if="mode === 'cdn'" class="cdn-field">
          <label class="field-label">CDN 域名或根 URL</label>
          <InputText
            v-model="cdnDomain"
            class="cdn-input"
            placeholder="例如：https://img.example.com 或 img.example.com"
            @update:model-value="clearSaveFeedback"
          />
          <p class="field-hint">可只填域名，将自动补全为 <code>https://</code>；保存后访客端与后台预览均使用该前缀。</p>
        </div>

        <div class="preview-block">
          <span class="preview-label">示例拼接结果</span>
          <code class="preview-code">{{ previewUrl }}</code>
        </div>
      </section>

      <section class="block block-spaced">
        <h3>访客分页</h3>
        <p class="hint">控制访客端瀑布流每次向后端请求的相册条数（1～100），并用于触底自动加载更多。</p>
        <div class="cdn-field">
          <label class="field-label">每页条数</label>
          <InputText
            v-model="feedPageSize"
            type="number"
            class="cdn-input feed-page-input"
            :min="1"
            :max="100"
            @update:model-value="clearSaveFeedback"
          />
        </div>
      </section>

      <section class="block block-spaced">
        <h3>热门相册</h3>
        <p class="hint">控制访客端首页热门横滑区展示数量（1～30）。热门区固定请求，不参与推荐瀑布流分页。</p>
        <div class="cdn-field">
          <label class="field-label">热门数量</label>
          <InputText
            v-model="hotAlbumSize"
            type="number"
            class="cdn-input feed-page-input"
            :min="1"
            :max="30"
            @update:model-value="clearSaveFeedback"
          />
        </div>
      </section>

      <section class="block block-spaced">
        <h3>访客首页顶部</h3>
        <p class="hint">
          配置访客端首页顶部标题与副标题；「Header 脚本」会注入到访客站点 <code>&lt;head&gt;</code>（支持完整
          <code>&lt;script&gt;</code> 标签或纯 JS 片段，仅管理员可编辑，请谨慎填写）。
        </p>
        <div class="cdn-field">
          <label class="field-label">顶部标题</label>
          <InputText
            v-model="siteTopTitle"
            class="cdn-input"
            placeholder="例如：相册"
            maxlength="200"
            @update:model-value="clearSaveFeedback"
          />
        </div>
        <div class="cdn-field">
          <label class="field-label">顶部描述</label>
          <Textarea
            v-model="siteTopDescription"
            class="cdn-input textarea-input"
            rows="3"
            auto-resize
            placeholder="例如：记录生活，珍藏美好"
            maxlength="2000"
            @update:model-value="clearSaveFeedback"
          />
        </div>
        <div class="cdn-field">
          <label class="field-label">Header 脚本内容</label>
          <Textarea
            v-model="siteHeaderScript"
            class="cdn-input textarea-input textarea-script"
            rows="8"
            auto-resize
            placeholder="例如：统计脚本或 &lt;script src=&quot;...&quot;&gt;&lt;/script&gt;"
            @update:model-value="clearSaveFeedback"
          />
        </div>
      </section>

      <section class="block block-spaced save-all-block">
        <p v-if="saveError" class="err">{{ saveError }}</p>
        <div class="actions">
          <Button
            label="保存全部配置"
            icon="pi pi-save"
            :loading="saving"
            :disabled="loading"
            @click="onSaveAll"
          />
          <span v-if="saveOkHint" class="ok">{{ saveOkHint }}</span>
        </div>
        <p class="field-hint save-all-hint">将依次保存 CDN、分页、热门数量、首页顶部与 Header 脚本；任一步失败则中止并提示错误。</p>
      </section>
    </article>
  </AdminLayout>
</template>

<style scoped>
.panel h2 {
  margin-top: 0;
}
.hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}
.hint code {
  font-size: 12px;
  background: #f1f5f9;
  padding: 1px 6px;
  border-radius: 4px;
}
.block {
  max-width: 640px;
}
.block-spaced {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;
}
.block h3 {
  margin: 0 0 12px;
  font-size: 1rem;
}
.save-all-block {
  margin-top: 32px;
  padding-top: 20px;
  border-top: 2px solid #cbd5e1;
}
.save-all-hint {
  margin-top: 10px;
  margin-bottom: 0;
}
.mode-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
}
.mode-option {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #334155;
}
.cdn-field {
  margin-bottom: 16px;
}
.field-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
}
.cdn-input {
  width: 100%;
  max-width: 480px;
}
.feed-page-input {
  max-width: 160px;
}
.textarea-input {
  max-width: 640px;
  min-height: 72px;
  resize: vertical;
}
.textarea-script {
  min-height: 160px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}
.field-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.45;
}
.field-hint code {
  font-size: 11px;
  background: #f1f5f9;
  padding: 1px 5px;
  border-radius: 3px;
}
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.ok {
  font-size: 13px;
  color: #15803d;
}
.err {
  margin: 0 0 12px;
  font-size: 13px;
  color: #b91c1c;
}
.preview-block {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.preview-label {
  font-size: 12px;
  color: #64748b;
}
.preview-code {
  display: block;
  font-size: 12px;
  word-break: break-all;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 12px;
  color: #0f172a;
}
</style>
