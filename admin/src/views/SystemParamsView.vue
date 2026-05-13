<script setup>
import { computed, onMounted, ref } from 'vue'
import AdminLayout from '../modules/dashboard/components/AdminLayout.vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import {
  fetchImageCdnSetting,
  saveImageCdnSetting,
  fetchFeedPageSetting,
  saveFeedPageSetting,
  fetchHotAlbumSizeSetting,
  saveHotAlbumSizeSetting
} from '../api/systemSettingsApi'
import { getImageCdnBase, setImageCdnBase } from '../utils/assetUrl'

const mode = ref('local')
const cdnDomain = ref('')
const feedPageSize = ref(8)
const hotAlbumSize = ref(8)

const loading = ref(false)
const savingCdn = ref(false)
const savingFeed = ref(false)
const savingHot = ref(false)
const cdnError = ref('')
const feedError = ref('')
const hotError = ref('')
const cdnSavedHint = ref('')
const feedSavedHint = ref('')
const hotSavedHint = ref('')

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
  cdnError.value = ''
  feedError.value = ''
  try {
    const [cdn, feed, hot] = await Promise.all([
      fetchImageCdnSetting(),
      fetchFeedPageSetting(),
      fetchHotAlbumSizeSetting()
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
  } catch (e) {
    cdnError.value = e?.response?.data?.message || e?.message || '加载配置失败'
  } finally {
    loading.value = false
  }
})

const onModeChange = (next) => {
  mode.value = next
  cdnSavedHint.value = ''
  if (next === 'local') {
    cdnDomain.value = ''
  } else if (!cdnDomain.value.trim() && getImageCdnBase()) {
    cdnDomain.value = getImageCdnBase()
  }
}

const onSaveCdn = async () => {
  savingCdn.value = true
  cdnError.value = ''
  cdnSavedHint.value = ''
  const payload = mode.value === 'local' ? '' : cdnDomain.value.trim()
  try {
    await saveImageCdnSetting(payload)
    cdnSavedHint.value = '已保存'
  } catch (e) {
    cdnError.value = e?.response?.data?.message || e?.message || '保存失败'
  } finally {
    savingCdn.value = false
  }
}

const onSaveFeedPage = async () => {
  savingFeed.value = true
  feedError.value = ''
  feedSavedHint.value = ''
  const raw = parseInt(String(feedPageSize.value).trim(), 10)
  if (!Number.isFinite(raw) || raw < 1 || raw > 100) {
    feedError.value = '请输入 1～100 之间的整数'
    savingFeed.value = false
    return
  }
  try {
    const out = await saveFeedPageSetting(raw)
    const stored = out?.feedPageSize
    if (typeof stored === 'number') {
      feedPageSize.value = stored
    }
    feedSavedHint.value = '已保存'
  } catch (e) {
    feedError.value = e?.response?.data?.message || e?.message || '保存失败'
  } finally {
    savingFeed.value = false
  }
}

const onSaveHotAlbumSize = async () => {
  savingHot.value = true
  hotError.value = ''
  hotSavedHint.value = ''
  const raw = parseInt(String(hotAlbumSize.value).trim(), 10)
  if (!Number.isFinite(raw) || raw < 1 || raw > 30) {
    hotError.value = '请输入 1～30 之间的整数'
    savingHot.value = false
    return
  }
  try {
    const out = await saveHotAlbumSizeSetting(raw)
    const stored = out?.hotAlbumSize
    if (typeof stored === 'number') {
      hotAlbumSize.value = stored
    }
    hotSavedHint.value = '已保存'
  } catch (e) {
    hotError.value = e?.response?.data?.message || e?.message || '保存失败'
  } finally {
    savingHot.value = false
  }
}
</script>

<template>
  <AdminLayout :loading="loading">
    <article class="panel full">
      <h2>系统参数配置</h2>

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
            @update:model-value="cdnSavedHint = ''"
          />
          <p class="field-hint">可只填域名，将自动补全为 <code>https://</code>；保存后访客端与后台预览均使用该前缀。</p>
        </div>

        <div class="actions">
          <Button label="保存 CDN 设置" icon="pi pi-check" :loading="savingCdn" :disabled="loading" @click="onSaveCdn" />
          <span v-if="cdnSavedHint" class="ok">{{ cdnSavedHint }}</span>
        </div>
        <p v-if="cdnError" class="err">{{ cdnError }}</p>

        <div class="preview-block">
          <span class="preview-label">示例拼接结果</span>
          <code class="preview-code">{{ previewUrl }}</code>
        </div>
      </section>

      <section class="block block-spaced">
        <h3>访客分页</h3>
        <p class="hint">控制访客端「名人榜」瀑布流每次向后端请求的相册条数（1～100），并用于触底自动加载更多。</p>
        <div class="cdn-field">
          <label class="field-label">每页条数</label>
          <InputText
            v-model="feedPageSize"
            type="number"
            class="cdn-input feed-page-input"
            :min="1"
            :max="100"
            @update:model-value="feedSavedHint = ''"
          />
        </div>
        <div class="actions">
          <Button label="保存分页设置" icon="pi pi-save" :loading="savingFeed" :disabled="loading" @click="onSaveFeedPage" />
          <span v-if="feedSavedHint" class="ok">{{ feedSavedHint }}</span>
        </div>
        <p v-if="feedError" class="err">{{ feedError }}</p>
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
            @update:model-value="hotSavedHint = ''"
          />
        </div>
        <div class="actions">
          <Button label="保存热门设置" icon="pi pi-save" :loading="savingHot" :disabled="loading" @click="onSaveHotAlbumSize" />
          <span v-if="hotSavedHint" class="ok">{{ hotSavedHint }}</span>
        </div>
        <p v-if="hotError" class="err">{{ hotError }}</p>
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
