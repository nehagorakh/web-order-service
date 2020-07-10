use lifcare_order;

ALTER TABLE `order` 
CHANGE COLUMN `updated_at` `updated_at` TIMESTAMP NULL;
ALTER TABLE `order`
ADD COLUMN `merged_by` VARCHAR(45) NULL DEFAULT NULL ,
ADD COLUMN `merged_by_name` VARCHAR(256) NULL DEFAULT NULL ;
ALTER TABLE `order` 
CHANGE COLUMN `updated_at` `updated_at` TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP ;


ALTER TABLE `order_item` 
ADD COLUMN `child_order_id` INT(20) NULL DEFAULT 0;

ALTER TABLE `order_item`
ADD COLUMN `facility_code` INT(20) NULL DEFAULT NULL ;

ALTER TABLE `cart_item` 
ADD COLUMN `facility_code` INT(20) NULL DEFAULT NULL;

ALTER TABLE `order` 
CHANGE COLUMN `updated_at` `updated_at` TIMESTAMP NULL;

ALTER TABLE `order`
ADD COLUMN `merged_by` VARCHAR(45) NULL DEFAULT NULL ,
ADD COLUMN `merged_by_name` VARCHAR(256) NULL DEFAULT NULL ;

ALTER TABLE `order` 
CHANGE COLUMN `updated_at` `updated_at` TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP ;

ALTER TABLE `order_item` 
ADD COLUMN `child_order_id` INT(20) NULL DEFAULT 0;

ALTER TABLE `order_item`
ADD COLUMN `facility_code` INT(20) NULL DEFAULT NULL ;

ALTER TABLE `cart_item` 
ADD COLUMN `facility_code` INT(20) NULL DEFAULT NULL;
