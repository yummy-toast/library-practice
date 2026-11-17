# Library Practice

用 Java 17、Spring Boot 和 MySQL 实现的图书借阅练习项目。

- `GET /books/B001` 返回图书详情。
- 查询不存在的图书编号时返回 `404`。

## Redis 图书详情缓存

本地 Redis 地址为 `127.0.0.1:26379`。

- 首次查询图书时从 MySQL 读取数据，将书名和作者写入 Redis，有效期为 5 分钟。
- 缓存命中时从 Redis 读取书名和作者，库存仍从 MySQL 查询。
- 测试归还借阅记录后，接口返回的库存立即从 `0` 变为 `1`，详情缓存仍然有效。

查询 `GET /books/B001` 后，可以用 `redis-cli` 检查缓存内容和剩余有效期：

    redis-cli -p 26379 HGETALL library:book:details:B001
    redis-cli -p 26379 TTL library:book:details:B001

## 接口验收

参见[借阅与归还验收](ACCEPTANCE.md)，按步骤验证库存不足、重复归还、并发借阅和事务回滚。