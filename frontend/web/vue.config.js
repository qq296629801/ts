// Docker 内用服务名；宿主机本地开发用 localhost
const gatewayTarget = process.env.VUE_APP_PROXY_GATEWAY || 'http://localhost:8081'
const platformTarget = process.env.VUE_APP_PROXY_PLATFORM || 'http://localhost:8080'

module.exports = {
  devServer: {
    port: 8082,
    proxy: {
      '/api/v1/ai/chat': { target: gatewayTarget, changeOrigin: true },
      '/api/v1/ai/image': { target: gatewayTarget, changeOrigin: true },
      '/api': { target: platformTarget, changeOrigin: true }
    }
  }
}
