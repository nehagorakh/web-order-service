use lifcare_order;
ALTER TABLE cart ADD COLUMN `category` VARCHAR(45) DEFAULT "MEDICINE" AFTER source, ADD COLUMN `appointment_date` DATETIME DEFAULT NULL AFTER category,
ADD COLUMN `appointment_slot_id` int(20) DEFAULT NULL AFTER appointment_date,
ADD COLUMN `mobile` VARCHAR(255) DEFAULT NULL AFTER patient_last_name,
                            ADD COLUMN `email` VARCHAR(255) DEFAULT NULL AFTER mobile,
ADD COLUMN `appointment_slot` VARCHAR(255) DEFAULT NULL AFTER appointment_slot_id,
ADD COLUMN user_type VARCHAR(45) DEFAULT NULL AFTER appointment_slot,
ADD COLUMN `report_delivery_charge` DOUBLE DEFAULT 0.0 AFTER appointment_slot,
ADD COLUMN `is_report_hard_copy_required` TINYINT DEFAULT 0 AFTER report_delivery_charge;

update cart set user_type = "INTERNAL" where source="JIVA";
update cart set user_type = "EXTERNAL" where source!= "JIVA";


ALTER TABLE `order`
 ADD COLUMN `category` VARCHAR(45) DEFAULT "MEDICINE"
 AFTER source,
 		ADD COLUMN `appointment_date` DATETIME DEFAULT NULL
 AFTER category,
 ADD COLUMN `appointment_slot` VARCHAR(255) DEFAULT NULL
 AFTER appointment_date,
 ADD COLUMN appointment_slot_id INT(20) DEFAULT NULL After appointment_date,
 ADD COLUMN appointment_id int(11) AFTER appointment_slot,
ADD COLUMN `report_delivery_charge` DOUBLE DEFAULT 0.0 AFTER appointment_slot,
ADD COLUMN `is_report_hard_copy_required` TINYINT DEFAULT 0 AFTER report_delivery_charge
ADD COLUMN `created_by_name` VARCHAR(255) DEFAULT NULL
 AFTER created_by,
ADD COLUMN `updated_by_name` VARCHAR(255) DEFAULT NULL
 AFTER created_by_name;

ALTER TABLE cart_item
 ADD COLUMN patient_id BIGINT(20),
 ADD COLUMN `product_category` VARCHAR(45) DEFAULT "MEDICINE"
 AFTER patient_id,
ADD COLUMN `product_sub_category` VARCHAR(45) DEFAULT "MEDICINE"
 AFTER patient_id,
ADD COLUMN `is_bundled_product`  int(20) DEFAULT false
 AFTER patient_id,
 ADD COLUMN `patient_first_name` VARCHAR(255) DEFAULT NULL after patient_id,
 ADD COLUMN `patient_last_name` VARCHAR(255) DEFAULT NULL after patient_first_name;

ALTER TABLE order_item
 ADD COLUMN patient_id BIGINT(20),
 ADD COLUMN `product_category` VARCHAR(45) DEFAULT "MEDICINE"
 AFTER patient_id,
 ADD COLUMN `patient_first_name` VARCHAR(255) DEFAULT NULL after patient_id,
 ADD COLUMN `patient_last_name` VARCHAR(255) DEFAULT NULL after patient_first_name;

ALTER TABLE shipping_address ADD column email varchar(500) default Null after mobile;