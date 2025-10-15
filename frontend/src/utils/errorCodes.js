export const CODE_OK = 200;

// 业务码 → 友好提示文案
export const CODE_MESSAGES = {
  1000: '参数校验失败',
  1001: '用户名或密码错误',
  1002: '用户不存在或被禁用',
  1003: '注册失败',
  1004: '用户名已存在',
  1005: '邮箱已存在',
  500: '服务内部错误'
};

export function messageOf(code, fallback) {
  return CODE_MESSAGES[code] || fallback || '请求失败';
}
