<script setup>
import { computed, onMounted, ref } from 'vue'
import AdminLayout from '../modules/dashboard/components/AdminLayout.vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import { fetchImageCdnSetting, saveImageCdnSetting } from '../api/systemSettingsApi'
import { getImageCdnBase, setImageCdnBase } from '../utils/assetUrl'

const mode = ref('local')
const cdnDomain = ref('')
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const savedHint = ref('')

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

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    const { imageCdnBase } = await fetchImageCdnSetting()
    const base = typeof imageCdnBase === 'string' ? imageCdnBase : ''
    setImageCdnBase(base)
    if (base) {
      mode.value = 'cdn'
      cdnDomain.value = base
    } else {
      mode.value = 'local'
      cdnDomain.value = ''
    }
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '加载配置失败'
  } finally {
    loading.value = false
  }
})

const onModeChange = (next) => {
  mode.value = next
  savedHint.value = ''
  if (next === 'local') {
    cdnDomain.value = ''
  } else if (!cdnDomain.value.trim() && getImageCdnBase()) {
    cdnDomain.value = getImageCdnBase()
  }
}

const onSave = async () => {
  saving.value = true
  error.value = ''
  savedHint.value = ''
  const payload = mode.value === 'local' ? '' : cdnDomain.value.trim()
  try {
    await saveImageCdnSetting(payload)
    savedHint.value = '已保存'
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '保存失败'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <AdminLayout :loading="loading">
    <article class="panel full">
      <h2>系统参数配置</h2>
      <p class="hint">图片资源地址：选择本地（与接口同源）或由 CDN 域名拼接 <code>/uploads/...</code> 路径。</p>

      <section class="block">
        <h3>图片 CDN</h3>
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
            @update:model-value="savedHint = ''"
          />
          <p class="field-hint">可只填域名，将自动补全为 <code>https://</code>；保存后访客端与后台预览均使用该前缀。</p>
        </div>

        <div class="actions">
          <Button label="保存" icon="pi pi-check" :loading="saving" :disabled="loading" @click="onSave" />
          <span v-if="savedHint" class="ok">{{ savedHint }}</span>
        </div>
        <p v-if="error" class="err">{{ error }}</p>

        <div class="preview-block">
          <span class="preview-label">示例拼接结果</span>
          <code class="preview-code">{{ previewUrl }}</code>
        </div>
      </section>
    </article>
  </AdminLayout>
</template>

<style scoped>
.panel h2 {
  margin-top: 0;
}
.hint {
  margin: 0 0 20px;
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
.block h3 {
  margin: 0 0 12px;
  font-size: 1rem;
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
  margin-bottom: 8px;
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
