<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue';
import { userApi } from '@/api/modules';
import { useUserStore } from '@/stores/user';

const props = defineProps<{
  visible: boolean;
}>();

const emit = defineEmits(['update:visible', 'saved']);

const userStore = useUserStore();
const activeTab = ref('profile');
const loading = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);

// 个人资料
const profileForm = reactive({
  nickname: '',
  email: '',
  avatar: ''
});

// 修改密码
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

function syncProfile() {
  if (userStore.profile) {
    profileForm.nickname = userStore.profile.nickname || '';
    profileForm.email = userStore.profile.email || '';
    profileForm.avatar = userStore.profile.avatar || '';
  }
}

onMounted(() => {
  syncProfile();
});

// 监听可见性，打开时同步最新数据
watch(() => props.visible, (val) => {
  if (val) syncProfile();
});

function handleClose() {
  emit('update:visible', false);
}

// 资料保存
async function saveProfile() {
  if (!profileForm.nickname.trim()) return;
  loading.value = true;
  try {
    await userApi.updateProfile({ displayName: profileForm.nickname });
    // 更新本地 store
    userStore.updateProfile({ nickname: profileForm.nickname });
    alert('资料修改成功');
  } catch (e: any) {
    alert(e.message || '修改失败');
  } finally {
    loading.value = false;
  }
}

// 头像上传
function triggerUpload() {
  fileInput.value?.click();
}

async function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  // 类型校验
  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件');
    return;
  }

  loading.value = true;
  try {
    const res = await userApi.uploadAvatar(file);
    const newAvatar = res.data; // res.data 是字符串 URL
    profileForm.avatar = newAvatar;
    // 更新本地 store
    userStore.updateProfile({ avatar: newAvatar });
    alert('头像上传成功');
  } catch (e: any) {
    alert(e.message || '上传失败');
  } finally {
    loading.value = false;
    // 重置 input
    target.value = '';
  }
}

// 密码修改
async function changePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    alert('请填写完整信息');
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    alert('两次新密码输入不一致');
    return;
  }

  loading.value = true;
  try {
    await userApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    });
    alert('密码修改成功');
    // 清空表单
    passwordForm.oldPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
  } catch (e: any) {
    alert(e.message || '修改失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <Transition name="fade">
    <div v-if="visible" class="modal-overlay" @click.self="handleClose">
      <div class="modal-container glass">
        <div class="modal-header">
          <h2 class="modal-title">设置</h2>
          <button class="close-btn" @click="handleClose">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="modal-content">
          <aside class="modal-sidebar">
            <button 
              class="tab-item" 
              :class="{ active: activeTab === 'profile' }"
              @click="activeTab = 'profile'"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              个人资料
            </button>
            <button 
              class="tab-item" 
              :class="{ active: activeTab === 'security' }"
              @click="activeTab = 'security'"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              密码安全
            </button>
          </aside>

          <main class="modal-main">
            <!-- 个人资料 Tab -->
            <div v-if="activeTab === 'profile'" class="tab-pane">
              <div class="profile-header">
                <div class="avatar-wrapper" @click="triggerUpload">
                  <img 
                    v-if="profileForm.avatar" 
                    :src="profileForm.avatar" 
                    class="avatar-img"
                  />
                  <div v-else class="avatar-placeholder">
                    {{ profileForm.nickname?.charAt(0) || 'U' }}
                  </div>
                  <div class="avatar-overlay">
                    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
                      <circle cx="12" cy="13" r="4" />
                    </svg>
                  </div>
                  <input 
                    type="file" 
                    ref="fileInput" 
                    class="hidden-input" 
                    accept="image/*"
                    @change="handleFileChange"
                  />
                </div>
                <div class="profile-info">
                  <h3>{{ profileForm.nickname }}</h3>
                  <p>{{ profileForm.email }}</p>
                </div>
              </div>

              <div class="form-group">
                <label>显示名称</label>
                <input 
                  type="text" 
                  v-model="profileForm.nickname" 
                  placeholder="请输入昵称"
                  class="input-control"
                />
              </div>

              <div class="form-group">
                <label>电子邮箱</label>
                <input 
                  type="text" 
                  v-model="profileForm.email" 
                  disabled
                  class="input-control disabled"
                />
                <span class="hint">邮箱地址暂不支持修改</span>
              </div>

              <div class="form-actions">
                <button 
                  class="btn-primary" 
                  :disabled="loading" 
                  @click="saveProfile"
                >
                  {{ loading ? '保存中...' : '保存修改' }}
                </button>
              </div>
            </div>

            <!-- 安全设置 Tab -->
            <div v-else-if="activeTab === 'security'" class="tab-pane">
              <div class="form-group">
                <label>当前密码</label>
                <input 
                  type="password" 
                  v-model="passwordForm.oldPassword" 
                  placeholder="请输入当前使用的密码"
                  class="input-control"
                />
              </div>

              <div class="form-group border-top">
                <label>新密码</label>
                <input 
                  type="password" 
                  v-model="passwordForm.newPassword" 
                  placeholder="请输入新密码"
                  class="input-control"
                />
              </div>

              <div class="form-group">
                <label>确认新密码</label>
                <input 
                  type="password" 
                  v-model="passwordForm.confirmPassword" 
                  placeholder="请再次输入新密码"
                  class="input-control"
                />
              </div>

              <div class="form-actions">
                <button 
                  class="btn-primary" 
                  :disabled="loading" 
                  @click="changePassword"
                >
                  {{ loading ? '修改中...' : '更新密码' }}
                </button>
              </div>
            </div>
          </main>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
/* 保持原样式不变，此处略去重复部分以节省空间，但在实际写入时会完整保留 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-container {
  width: 720px;
  height: 540px;
  background: var(--bg-primary);
  border: 1px solid var(--border-primary);
  border-radius: 20px;
  box-shadow: var(--shadow-2xl);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modal-in 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes modal-in {
  from { opacity: 0; transform: scale(0.9) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.modal-header {
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-primary);
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.close-btn {
  background: transparent;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.modal-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.modal-sidebar {
  width: 200px;
  padding: 16px 8px;
  border-right: 1px solid var(--border-primary);
  background: var(--bg-secondary);
}

.tab-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border: none;
  background: transparent;
  border-radius: 10px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.tab-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.tab-item.active {
  background: var(--brand-primary);
  background: rgba(59, 130, 246, 0.1);
  color: var(--brand-primary);
  font-weight: 500;
}

.modal-main {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 40px;
}

.avatar-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
  border: 4px solid var(--bg-secondary);
  box-shadow: var(--shadow-md);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: var(--brand-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 600;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.profile-info h3 {
  margin: 0 0 4px;
  font-size: 18px;
}

.profile-info p {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 14px;
}

.form-group {
  margin-bottom: 24px;
}

.form-group.border-top {
  margin-top: 32px;
  padding-top: 32px;
  border-top: 1px solid var(--border-primary);
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--text-secondary);
}

.input-control {
  width: 100%;
  padding: 10px 14px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-primary);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 14px;
  transition: all 0.2s;
}

.input-control:focus {
  outline: none;
  border-color: var(--brand-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.input-control.disabled {
  background: var(--bg-hover);
  cursor: not-allowed;
  color: var(--text-tertiary);
}

.hint {
  display: block;
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 6px;
}

.form-actions {
  margin-top: 40px;
  display: flex;
  justify-content: flex-end;
}

.btn-primary {
  padding: 10px 24px;
  background: var(--brand-primary);
  color: white;
  border: none;
  border-radius: 10px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.hidden-input {
  display: none;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
