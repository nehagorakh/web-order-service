ALTER TABLE lifcare_order.order add column coupon_cashback float(8,2) default 0 after coupon_discount;

ALTER TABLE lifcare_order.order add column redeemed_coupon_cashback float(8,2) default 0 after coupon_cashback;

ALTER TABLE lifcare_order.cart add column coupon_cashback float(8,2) default 0 after coupon_discount;