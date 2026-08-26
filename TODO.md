# TODO

- [ ] 将数据库连接信息和其他密钥从 `backend/src/main/resources/application.yml` 迁移到环境变量或部署平台密钥，并轮换当前数据库密码。
- [ ] 收敛后端认证边界：避免只依赖 `/api/**` 的 Shiro `anon` 配置加 service 层手动校验；改为显式保护需要登录的 API 路径，或补充清晰的 Shiro/session 策略说明。
