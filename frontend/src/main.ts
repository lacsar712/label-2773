import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import router from './router';
import { createPinia } from 'pinia';
import { vPermission } from './directives/permission';

const pinia = createPinia();
const app = createApp(App);

app.use(Antd);
app.use(pinia);
app.use(router);
app.directive('permission', vPermission);
app.mount('#app');
