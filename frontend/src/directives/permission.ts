import { useAuthStore } from '../stores/auth';

export const vPermission = {
  mounted(el: HTMLElement, binding: any) {
    const { value } = binding;
    const authStore = useAuthStore();

    if (value && Array.isArray(value) && value.length > 0) {
      const hasPermission = authStore.hasRole(value);
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el);
      }
    }
  },
};

export function hasPermission(roles: string | string[]): boolean {
  const authStore = useAuthStore();
  return authStore.hasRole(roles);
}
