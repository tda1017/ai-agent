/**
 * 模拟 SSE 流式数据，用于后端未就绪时的开发调试。
 */
export function simulateStream(chunks, { onChunk, onComplete, onError, interval = 500 } = {}) {
  let stopped = false;
  const timers = [];

  chunks.forEach((chunk, index) => {
    const timer = setTimeout(() => {
      if (stopped) {
        return;
      }
      try {
        const payload = typeof chunk === 'function' ? chunk() : chunk;
        onChunk?.(payload);
        if (index === chunks.length - 1) {
          onComplete?.();
        }
      } catch (error) {
        console.error('模拟 SSE 流发生异常', error);
        onError?.(error);
      }
    }, interval * (index + 1));
    timers.push(timer);
  });

  return () => {
    stopped = true;
    timers.forEach(clearTimeout);
  };
}