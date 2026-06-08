<template>
  <div class="change-password-container">
    <div class="change-password-card">
      <div class="card-header">
        <div class="header-icon">
          <LockOutlined />
        </div>
        <h2 class="card-title">修改密码</h2>
        <p class="card-subtitle" v-if="isFirstLogin">
          首次登录，为了账户安全，请先修改密码
        </p>
        <p class="card-subtitle" v-else>
          请输入您的旧密码和新密码
        </p>
      </div>
      <a-form
        :model="passwordForm"
        :rules="rules"
        layout="vertical"
        @finish="handleChangePassword"
        ref="formRef"
      >
        <a-form-item name="oldPassword" label="旧密码">
          <a-input-password
            v-model:value="passwordForm.oldPassword"
            size="large"
            placeholder="请输入旧密码"
          >
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>
        <a-form-item name="newPassword" label="新密码">
          <a-input-password
            v-model:value="passwordForm.newPassword"
            size="large"
            placeholder="请输入新密码（6-20位）"
          >
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>
        <a-form-item name="confirmPassword" label="确认新密码">
          <a-input-password
            v-model:value="passwordForm.confirmPassword"
            size="large"
            placeholder="请再次输入新密码"
            @pressEnter="handleSubmit"
          >
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>
        <div class="password-strength" v-if="passwordForm.newPassword">
          <div class="strength-label">密码强度</div>
          <div class="strength-bar">
            <div
              class="strength-fill"
              :class="strengthClass"
              :style="{ width: strengthWidth }"
            ></div>
          </div>
          <div class="strength-text" :class="strengthClass">
            {{ strengthText }}
          </div>
        </div>
        <div class="form-actions">
          <a-button
            v-if="!isFirstLogin"
            size="large"
            @click="goBack"
          >
            返回
          </a-button>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            :loading="loading"
            class="submit-btn"
          >
            {{ loading ? '修改中...' : '确认修改' }}
          </a-button>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { message } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue';
import { LockOutlined } from '@ant-design/icons-vue';

const authStore = useAuthStore();
const router = useRouter();

const formRef = ref<FormInstance>();
const loading = ref(false);
const isFirstLogin = ref(false);

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const validateConfirmPassword = async (_rule: any, value: string) => {
  if (!value) {
    return Promise.reject('请再次输入新密码');
  }
  if (value !== passwordForm.newPassword) {
    return Promise.reject('两次输入的密码不一致');
  }
  return Promise.resolve();
};

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为6-20位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' },
  ],
};

const passwordStrength = computed(() => {
  const pwd = passwordForm.newPassword;
  if (!pwd) return 0;
  let score = 0;
  if (pwd.length >= 6) score++;
  if (pwd.length >= 10) score++;
  if (/[A-Z]/.test(pwd)) score++;
  if (/[0-9]/.test(pwd)) score++;
  if (/[^A-Za-z0-9]/.test(pwd)) score++;
  return score;
});

const strengthWidth = computed(() => {
  const max = 5;
  return `${(passwordStrength.value / max) * 100}%`;
});

const strengthClass = computed(() => {
  const s = passwordStrength.value;
  if (s <= 1) return 'weak';
  if (s <= 3) return 'medium';
  return 'strong';
});

const strengthText = computed(() => {
  const s = passwordStrength.value;
  if (s <= 1) return '弱';
  if (s <= 3) return '中等';
  return '强';
});

const handleChangePassword = async () => {
  loading.value = true;
  try {
    await authStore.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    });
    if (isFirstLogin.value) {
      message.success('密码修改成功，请重新登录');
      authStore.logout();
      router.push('/login');
    } else {
      message.success('密码修改成功');
      router.back();
    }
  } catch (e) {
  } finally {
    loading.value = false;
  }
};

const handleSubmit = async () => {
  try {
    await formRef.value?.validate();
    await handleChangePassword();
  } catch {
    // validation failed
  }
};

const goBack = () => {
  router.back();
};

onMounted(() => {
  if (router.currentRoute.value.query.first === '1') {
    isFirstLogin.value = true;
  }
});
</script>

<style scoped lang="scss">
.change-password-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf3 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.change-password-card {
  width: 100%;
  max-width: 480px;
  background: #fff;
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);

  .card-header {
    text-align: center;
    margin-bottom: 32px;

    .header-icon {
      width: 64px;
      height: 64px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 28px;
      margin: 0 auto 16px;
    }

    .card-title {
      font-size: 24px;
      font-weight: 700;
      color: #262626;
      margin: 0 0 8px 0;
    }

    .card-subtitle {
      font-size: 14px;
      color: #8c8c8c;
      margin: 0;
    }
  }

  .password-strength {
    margin: -8px 0 24px 0;
    padding: 16px;
    background: #fafafa;
    border-radius: 8px;

    .strength-label {
      font-size: 12px;
      color: #8c8c8c;
      margin-bottom: 8px;
    }

    .strength-bar {
      height: 6px;
      background: #e8e8e8;
      border-radius: 3px;
      overflow: hidden;
      margin-bottom: 8px;

      .strength-fill {
        height: 100%;
        border-radius: 3px;
        transition: all 0.3s ease;

        &.weak {
          background: #f5222d;
        }
        &.medium {
          background: #faad14;
        }
        &.strong {
          background: #52c41a;
        }
      }
    }

    .strength-text {
      font-size: 12px;
      font-weight: 600;

      &.weak {
        color: #f5222d;
      }
      &.medium {
        color: #faad14;
      }
      &.strong {
        color: #52c41a;
      }
    }
  }

  .form-actions {
    display: flex;
    gap: 12px;
    margin-top: 24px;

    .submit-btn {
      flex: 1;
      height: 44px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border: none;
      font-weight: 600;
      font-size: 16px;
      border-radius: 8px;

      &:hover {
        opacity: 0.9;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }
    }
  }
}
</style>
