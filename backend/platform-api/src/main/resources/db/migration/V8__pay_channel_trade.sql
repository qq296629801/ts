-- 004：通用渠道交易号（支付宝/微信）
ALTER TABLE t_pay_order ADD COLUMN channel_trade_no VARCHAR(64) NULL AFTER wx_transaction_id;
