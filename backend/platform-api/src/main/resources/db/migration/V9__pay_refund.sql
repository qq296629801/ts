ALTER TABLE t_pay_order ADD COLUMN refunded_at DATETIME NULL;
ALTER TABLE t_pay_order ADD COLUMN refund_notify_id VARCHAR(128) NULL;
