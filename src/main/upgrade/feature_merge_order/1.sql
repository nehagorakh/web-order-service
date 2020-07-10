use lifcare_order;
ALTER TABLE `qa_lifcare_order`.`order` 
ADD COLUMN `merge_with_id` INT(21) NOT NULL DEFAULT 0 AFTER `email`;

