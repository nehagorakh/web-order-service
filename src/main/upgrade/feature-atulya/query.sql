Alter TABLE  lifcare_order.order add COLUMN  is_membership_added TINYINT(4) DEFAULT '0';

Alter TABLE  lifcare_order.cart add COLUMN  is_membership_added TINYINT(4) DEFAULT '0';