<template>
  <div class="login-container">
    <div class="login-wrapper">
      <div class="login-left">
        <div class="brand-section">
          <div class="brand-logo">
            <TeamOutlined />
          </div>
          <h1 class="brand-title">员工管理系统</h1>
          <p class="brand-subtitle">Employee Management System</p>
        </div>
        <div class="features-section">
          <div class="feature-item">
            <div class="feature-icon"><BarChartOutlined /></div>
            <div class="feature-text">
              <h4>数据概览</h4>
              <p>实时掌握企业人力数据</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon"><UserOutlined /></div>
            <div class="feature-text">
              <h4>员工管理</h4>
              <p>高效管理员工信息</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon"><ApartmentOutlined /></div>
            <div class="feature-text">
              <h4>部门管理</h4>
              <p>清晰组织架构管理</p>
            </div>
          </div>
        </div>
      </div>
      <div class="login-right">
        <div class="login-form-wrapper">
          <h2 class="form-title">欢迎登录</h2>
          <p class="form-subtitle">请输入您的账号信息</p>
          <a-form
            :model="loginForm"
            :rules="rules"
            layout="vertical"
            @finish="handleLogin"
          >
            <a-form-item name="username">
              <a-input
                v-model:value="loginForm.username"
                size="large"
                placeholder="请输入用户名"
                :prefix="userIcon"
              />
            </a-form-item>
            <a-form-item name="password">
              <a-input-password
                v-model:value="loginForm.password"
                size="large"
                placeholder="请输入密码"
                :prefix="lockIcon"
                @pressEnter="handleSubmit"
              />
            </a-form-item>
            <a-form-item name="captcha">
              <div class="captcha-wrapper">
                <a-input
                  v-model:value="loginForm.captcha"
                  size="large"
                  placeholder="请输入验证码"
                  :prefix="safetyIcon"
                  style="flex: 1;"
                />
                <div class="captcha-img" @click="refreshCaptcha" title="点击刷新">
                  <img v-if="captchaData.img" :src="captchaData.img" alt="验证码" />
                  <span v-else>加载中...</span>
                </div>
              </div>
            </a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              block
              :loading="loading"
              class="login-btn"
            >
              {{ loading ? '登录中...' : '登录' }}
            </a-button>
          </a-form>
          <div class="form-footer">
            <a-alert
              v-if="showHint"
              type="info"
              show-icon
              class="hint-alert"
            >
              <template #message>
                <div class="hint-content">
                  <p><strong>测试账号：</strong></p>
                  <p>管理员: admin / admin123</p>
                  <p>HR: hr / hr123456</p>
                  <p>员工: employee / emp123456</p>
                </div>
              </template>
            </a-alert>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, h } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { message } from 'ant-design-vue';
import {
  UserOutlined,
  LockOutlined,
  SafetyOutlined,
  TeamOutlined,
  BarChartOutlined,
  ApartmentOutlined,
} from '@ant-design/icons-vue';

const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();

const loading = ref(false);
const showHint = ref(true);
const captchaData = reactive({
  uuid: '',
  img: '',
});

const loginForm = reactive({
  username: '',
  password: '',
  captcha: '',
  uuid: '',
});

const userIcon = () => h(UserOutlined);
const lockIcon = () => h(LockOutlined);
const safetyIcon = () => h(SafetyOutlined);

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
};

const refreshCaptcha = async () => {
  try {
    const data = await authStore.getCaptcha();
    captchaData.uuid = data.uuid;
    captchaData.img = data.img;
    loginForm.uuid = data.uuid;
  } catch (e) {
    console.error('获取验证码失败', e);
  }
};

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password || !loginForm.captcha) {
    message.warning('请填写完整登录信息');
    return;
  }
  loading.value = true;
  try {
    const loginRes = await authStore.login({
      username: loginForm.username,
      password: loginForm.password,
      captcha: loginForm.captcha,
      uuid: loginForm.uuid,
    });
    message.success('登录成功');
    try {
      await authStore.fetchUserInfo();
    } catch (e) {
      console.error('获取用户信息失败', e);
    }
    if (loginRes.isFirstLogin) {
      router.push({ path: '/change-password', query: { first: '1' } });
      return;
    }
    const redirect = (route.query.redirect as string) || '/';
    router.push(redirect);
  } catch (e: any) {
    loginForm.captcha = '';
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
};

const handleSubmit = () => {
  handleLogin();
};

onMounted(() => {
  refreshCaptcha();
});
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-wrapper {
  display: flex;
  width: 100%;
  max-width: 1000px;
  min-height: 600px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 50px 40px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #fff;

  .brand-section {
    .brand-logo {
      width: 70px;
      height: 70px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 18px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 36px;
      margin-bottom: 20px;
      backdrop-filter: blur(10px);
    }

    .brand-title {
      font-size: 32px;
      font-weight: 700;
      margin: 0 0 8px 0;
      color: #fff;
    }

    .brand-subtitle {
      font-size: 14px;
      opacity: 0.8;
      margin: 0;
      letter-spacing: 2px;
    }
  }

  .features-section {
    display: flex;
    flex-direction: column;
    gap: 24px;

    .feature-item {
      display: flex;
      align-items: center;
      gap: 16px;

      .feature-icon {
        width: 48px;
        height: 48px;
        background: rgba(255, 255, 255, 0.15);
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;
        backdrop-filter: blur(10px);
      }

      .feature-text {
        h4 {
          margin: 0 0 4px 0;
          font-size: 16px;
          font-weight: 600;
          color: #fff;
        }

        p {
          margin: 0;
          font-size: 13px;
          opacity: 0.8;
        }
      }
    }
  }
}

.login-right {
  flex: 1;
  padding: 50px 50px;
  display: flex;
  align-items: center;
  justify-content: center;

  .login-form-wrapper {
    width: 100%;
    max-width: 360px;

    .form-title {
      font-size: 28px;
      font-weight: 700;
      color: #262626;
      margin: 0 0 8px 0;
    }

    .form-subtitle {
      font-size: 14px;
      color: #8c8c8c;
      margin: 0 0 32px 0;
    }

    .captcha-wrapper {
      display: flex;
      gap: 12px;
      align-items: center;

      .captcha-img {
        width: 120px;
        height: 40px;
        border: 1px solid #d9d9d9;
        border-radius: 6px;
        overflow: hidden;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #fafafa;
        transition: all 0.3s;

        &:hover {
          border-color: #667eea;
        }

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        span {
          font-size: 12px;
          color: #bfbfbf;
        }
      }
    }

    .login-btn {
      margin-top: 8px;
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

    .form-footer {
      margin-top: 24px;

      .hint-alert {
        border-radius: 8px;

        :deep(.ant-alert-content) {
          flex: 1;
        }

        .hint-content {
          font-size: 12px;
          line-height: 1.8;

          p {
            margin: 0;
          }

          strong {
            color: #262626;
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .login-wrapper {
    flex-direction: column;
    min-height: auto;
  }

  .login-left {
    padding: 40px 30px;
    min-height: 280px;

    .brand-section {
      .brand-title {
        font-size: 24px;
      }
    }

    .features-section {
      flex-direction: row;
      flex-wrap: wrap;
      gap: 16px;

      .feature-item {
        flex-basis: calc(50% - 8px);
      }
    }
  }

  .login-right {
    padding: 40px 30px;
  }
}

@media (max-width: 480px) {
  .login-left {
    .features-section {
      .feature-item {
        flex-basis: 100%;
      }
    }
  }
}
</style>
