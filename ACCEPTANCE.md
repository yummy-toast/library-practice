# 借阅与归还验收

在本地练习库 `library_practice` 中运行。启动 Spring Boot 后，先确认 `B001` 库存为 `1`、读者 `R001` 存在，并记下 `B001` 当前的借阅记录数。历史记录可以保留。

## 库存不足和重复归还

在显示 `%` 的终端执行下面的借阅命令两次：

```bash
curl -i -H 'Content-Type: application/json' \
  -d '{"bookID":"B001","readerID":"R001"}' \
  http://localhost:8080/loans
```

第一次应返回 `201` 和 `loanID`，第二次应返回 `409`。库存应为 `0`，借阅记录数只增加 `1`。

把下面的 `<loanID>` 替换成这次返回的数字，再执行两次：

```bash
curl -i -X POST 'http://localhost:8080/loans/<loanID>/return'
```

首次应返回 `204`，再次应返回 `409`，库存保持为 `1`。

## 并发借阅

库存为 `1` 时，把下面三行一起粘贴到显示 `%` 的终端：

```bash
curl -sS -w '\nA: HTTP %{http_code}\n' -H 'Content-Type: application/json' -d '{"bookID":"B001","readerID":"R001"}' http://localhost:8080/loans &
curl -sS -w '\nB: HTTP %{http_code}\n' -H 'Content-Type: application/json' -d '{"bookID":"B001","readerID":"R001"}' http://localhost:8080/loans &
wait
```

预期一个返回 `201`，一个返回 `409`。用下面的 SQL 核对库存为 `0`，且只新增一条借阅记录。随后归还刚生成的借阅，确认库存恢复为 `1`。

```sql
SELECT stockNum FROM book WHERE bookID = 'B001';
SELECT loanID, loanStatus FROM loan WHERE bookID = 'B001' ORDER BY loanID;
```

## 插入失败时回滚库存

确认库存为 `1`，并记录当前借阅记录数。仅在本地练习库的 `mysql>` 窗口执行：

```sql
DELIMITER //
CREATE TRIGGER acceptance_fail_loan_insert
BEFORE INSERT ON loan
FOR EACH ROW
BEGIN
    IF NEW.bookID = 'B001' AND NEW.readerID = 'R001' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Day 6 rollback test';
    END IF;
END//
DELIMITER ;
```

发一次相同的借阅请求。预期返回 `500`，Spring Boot 日志包含 `Day 6 rollback test`。在 `mysql>` 核对库存仍为 `1`，借阅记录数与测试前相同。无论结果如何，都执行最后一条清理触发器。

```sql
SELECT stockNum FROM book WHERE bookID = 'B001';
SELECT COUNT(*) AS loanCount FROM loan WHERE bookID = 'B001';
DROP TRIGGER acceptance_fail_loan_insert;
```

2026-11-17 实测时，日志记录了 `SQLSTATE 45000`，库存保持为 `1`，借阅记录保持为 `4` 条，触发器已清理。