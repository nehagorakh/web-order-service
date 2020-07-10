use lifcare_order;

ALTER TABLE `order` 
ADD COLUMN `procurement_type` VARCHAR(45) NULL DEFAULT 'NORMAL';


ALTER TABLE `cart` 
ADD COLUMN `procurement_type` VARCHAR(45) NULL DEFAULT 'NORMAL';

update  cart set procurement_type = 'NORMAL';

update  `order` set procurement_type = 'NORMAL';
