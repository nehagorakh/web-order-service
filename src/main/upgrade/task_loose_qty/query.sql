use <prefix>_lifcare_order;
alter table `order_item` add column loose_quantity int default 0 not null after quantity;
