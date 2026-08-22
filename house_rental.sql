/*
 Navicat Premium Data Transfer

 Source Server         : home
 Source Server Type    : MySQL
 Source Server Version : 80012
 Source Host           : localhost:3306
 Source Schema         : house_rental

 Target Server Type    : MySQL
 Target Server Version : 80012
 File Encoding         : 65001

 Date: 02/05/2026 22:58:07
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for appointment
-- ----------------------------
DROP TABLE IF EXISTS `appointment`;
CREATE TABLE `appointment`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `house_id` bigint(20) NOT NULL COMMENT '房源ID',
  `tenant_id` bigint(20) NOT NULL COMMENT '租客ID',
  `landlord_id` bigint(20) NOT NULL COMMENT '出租者ID',
  `appointment_time` datetime NOT NULL COMMENT '预约看房时间',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '状态：0待处理 1已同意 2已拒绝',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_appointment_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_appointment_landlord_id`(`landlord_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '看房预约表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of appointment
-- ----------------------------
INSERT INTO `appointment` VALUES (1, 4, 3, 2, '2026-03-20 14:00:00', 1, '周末下午方便看房', '2026-03-15 15:04:12', '2026-03-15 15:04:12');
INSERT INTO `appointment` VALUES (2, 2, 3, 2, '2026-03-09 00:00:00', 2, '尽快', '2026-03-15 22:31:07', '2026-03-15 22:31:07');
INSERT INTO `appointment` VALUES (3, 2, 7, 2, '2026-03-16 15:59:34', 1, '请尽快准备', '2026-03-16 15:59:48', '2026-03-16 15:59:48');

-- ----------------------------
-- Table structure for complaint
-- ----------------------------
DROP TABLE IF EXISTS `complaint`;
CREATE TABLE `complaint`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '投诉人ID',
  `target_id` bigint(20) NULL DEFAULT NULL COMMENT '被投诉对象ID',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '投诉内容',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '状态：0待处理 1已处理',
  `result` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '处理结果',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_complaint_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '投诉表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of complaint
-- ----------------------------
INSERT INTO `complaint` VALUES (1, 3, 2, '房东回复太慢，影响入住安排', 1, '已联系出租者沟通处理，请双方保持联系', '2026-03-15 19:54:22', '2026-03-15 19:54:22');

-- ----------------------------
-- Table structure for house
-- ----------------------------
DROP TABLE IF EXISTS `house`;
CREATE TABLE `house`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '房源标题',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '详细地址',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '城市',
  `area` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '区域',
  `rent_price` decimal(10, 2) NOT NULL COMMENT '月租金',
  `deposit` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '押金',
  `house_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '户型',
  `square` decimal(10, 2) NULL DEFAULT NULL COMMENT '面积',
  `floor` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '楼层',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '房源描述',
  `image_urls` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '房源图片，JSON数组或逗号分隔字符串',
  `longitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '经度（高德坐标）',
  `latitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '纬度（高德坐标）',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态：1上架 2待支付 3已出租 0下架',
  `audit_status` tinyint(4) NULL DEFAULT 0 COMMENT '审核状态：0待审核 1通过 2拒绝',
  `publisher_id` bigint(20) NOT NULL COMMENT '发布者ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_house_publisher_id`(`publisher_id` ASC) USING BTREE,
  INDEX `idx_house_audit_status`(`audit_status` ASC) USING BTREE,
  INDEX `idx_house_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_house_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '房源表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of house
-- ----------------------------
INSERT INTO `house` VALUES (1, '精装一室一厅', '朝阳区幸福路88号', '北京', '朝阳区', 3500.00, 3500.00, '一室一厅', 45.00, '8/18', '精装修，拎包入住', NULL, 116.486626, 39.915598, 3, 1, 2, '2026-03-15 14:26:36', '2026-03-15 14:26:36');
INSERT INTO `house` VALUES (2, '地铁口两室一厅', '海淀区科技路66号', '北京', '海淀区', 5200.00, 5200.00, '两室一厅', 78.00, '12/20', '靠近地铁，交通方便', NULL, 116.304511, 39.983424, 1, 1, 2, '2026-03-15 14:26:36', '2026-03-15 14:26:36');
INSERT INTO `house` VALUES (3, '精装公寓单间', '聊城市东昌府区大学路100号', '聊城', '东昌府区', 1500.00, 1500.00, '一室一厅', 45.00, '6/12', '精装修，拎包入住，交通方便，靠近学校，适合学生和单人居住。', NULL, 115.985371, 36.456703, 1, 1, 2, '2026-05-01 14:34:23', '2026-05-01 14:34:23');
INSERT INTO `house` VALUES (4, 'AI测试-大学旁精装单间', '聊城市东昌府区大学路100号', '聊城', '东昌府区', 1200.00, 1200.00, '单间', 28.00, '5/12', '靠近大学和学校，适合学生单人居住，精装修，拎包入住，周边公交交通方便。', NULL, 115.985371, 36.456703, 1, 1, 2, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (5, 'AI测试-考研安静一室一厅', '聊城市东昌府区学府路66号', '聊城', '东昌府区', 1500.00, 1500.00, '一室一厅', 45.00, '8/16', '小区环境安静，靠近高校，适合考研、学习和休息，房间采光好。', NULL, 115.986200, 36.459100, 1, 1, 2, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (6, 'AI测试-公交站旁温馨单间', '聊城市东昌府区柳园路88号', '聊城', '东昌府区', 1300.00, 1300.00, '单间', 30.00, '4/10', '楼下有公交站，交通方便，适合单人居住，周边生活配套较完善。', NULL, 115.978500, 36.448800, 1, 1, 2, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (7, 'AI测试-地铁口精装公寓', '聊城市东昌府区建设路120号', '聊城', '东昌府区', 1800.00, 1800.00, '一室一卫', 38.00, '10/18', '精装公寓，拎包入住，交通便利，靠近主路和公交站，适合上班通勤。', NULL, 115.990200, 36.451200, 1, 1, 5, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (8, 'AI测试-家庭两室一厅', '聊城市东昌府区花园路50号', '聊城', '东昌府区', 2200.00, 2200.00, '两室一厅', 75.00, '6/12', '两室一厅，面积适中，适合一家人居住，周边生活方便，配套齐全。', NULL, 115.972800, 36.443600, 1, 1, 5, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (9, 'AI测试-三室两厅家庭房', '聊城市东昌府区东昌路168号', '聊城', '东昌府区', 2800.00, 2800.00, '三室两厅', 105.00, '9/20', '三室两厅，面积较大，适合家庭居住，周边超市、学校和生活配套完善。', NULL, 115.994300, 36.462100, 1, 1, 5, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (10, 'AI测试-商圈旁两室公寓', '聊城市东昌府区万达商圈附近', '聊城', '东昌府区', 2100.00, 2100.00, '两室一厅', 68.00, '11/18', '靠近商圈，购物方便，生活配套成熟，适合上班族和小家庭居住。', NULL, 115.988300, 36.450500, 1, 1, 2, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (11, 'AI测试-低预算学生合租主卧', '聊城市东昌府区大学西路18号', '聊城', '东昌府区', 900.00, 900.00, '主卧', 22.00, '3/6', '低预算合租主卧，靠近高校，适合学生，交通方便，生活成本较低。', NULL, 115.982100, 36.457300, 1, 1, 2, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (12, 'AI测试-近学校一室公寓', '聊城市东昌府区学院路39号', '聊城', '东昌府区', 1400.00, 1400.00, '一室一厅', 42.00, '7/14', '近学校和学院，适合学生单人居住，房间整洁，周边公交方便。', NULL, 115.981700, 36.460200, 1, 1, 5, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (13, 'AI测试-电梯精装一室', '聊城市东昌府区新区路77号', '聊城', '东昌府区', 1600.00, 1600.00, '一室一厅', 48.00, '12/22', '电梯房，精装修，拎包入住，适合单人或情侣居住。', NULL, 115.997100, 36.449900, 1, 1, 5, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (14, 'AI测试-交通便利两室', '聊城市东昌府区昌润路66号', '聊城', '东昌府区', 1900.00, 1900.00, '两室一厅', 63.00, '5/11', '靠近主路和公交站，交通方便，通勤便利，适合上班族居住。', NULL, 115.973600, 36.452600, 1, 1, 2, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (15, 'AI测试-安静小区主卧', '聊城市东昌府区静雅小区3号楼', '聊城', '东昌府区', 1100.00, 1100.00, '主卧', 25.00, '2/6', '小区安静，适合学习、考研和休息，主卧采光好，生活便利。', NULL, 115.970900, 36.446900, 1, 1, 2, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (16, 'AI测试-高层阳光三室', '聊城市东昌府区阳光花园9号楼', '聊城', '东昌府区', 3000.00, 3000.00, '三室两厅', 115.00, '18/26', '高层采光好，三室两厅，面积大，适合家庭居住，周边生活配套齐全。', NULL, 115.991800, 36.465200, 1, 1, 5, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (17, 'AI测试-公园旁舒适一室', '聊城市东昌府区公园路21号', '聊城', '东昌府区', 1700.00, 1700.00, '一室一厅', 50.00, '6/15', '靠近公园，小区环境安静，适合休息和单人居住。', NULL, 115.976300, 36.461800, 1, 1, 5, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (18, 'AI测试-青年公寓单间', '聊城市东昌府区青年路28号', '聊城', '东昌府区', 1000.00, 1000.00, '单间', 24.00, '9/18', '青年公寓单间，精装，适合单人居住，交通方便，租金较低。', NULL, 115.979900, 36.454400, 1, 1, 2, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (19, 'AI测试-近医院电梯两室', '聊城市东昌府区人民医院附近', '聊城', '东昌府区', 2300.00, 2300.00, '两室一厅', 72.00, '13/20', '靠近医院和公交站，电梯房，生活方便，配套齐全，适合家庭居住。', NULL, 115.984200, 36.453300, 1, 1, 5, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (20, 'AI测试-地铁商圈小两居', '聊城市东昌府区商业街88号', '聊城', '东昌府区', 2400.00, 2400.00, '两室一厅', 70.00, '15/25', '靠近商圈，购物方便，交通便利，适合小家庭或上班族。', NULL, 115.989700, 36.447800, 1, 1, 2, '2026-05-01 16:02:06', '2026-05-01 16:02:06');
INSERT INTO `house` VALUES (21, 'AI测试-拎包入住精装两室', '聊城市东昌府区兴华路36号', '聊城', '东昌府区', 2000.00, 2000.00, '两室一厅', 66.00, '8/17', '两室精装修，拎包入住，装修较新，生活方便。', NULL, 115.981200, 36.451900, 1, 1, 5, '2026-05-01 16:02:06', '2026-05-01 16:02:06');

-- ----------------------------
-- Table structure for lease_contract
-- ----------------------------
DROP TABLE IF EXISTS `lease_contract`;
CREATE TABLE `lease_contract`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `house_id` bigint(20) NOT NULL COMMENT '房源ID',
  `tenant_id` bigint(20) NOT NULL COMMENT '租客ID',
  `landlord_id` bigint(20) NOT NULL COMMENT '出租者ID',
  `application_id` bigint(20) NULL DEFAULT NULL COMMENT '租房申请ID',
  `start_date` date NOT NULL COMMENT '合同开始日期',
  `end_date` date NOT NULL COMMENT '合同结束日期',
  `monthly_rent` decimal(10, 2) NOT NULL COMMENT '月租金',
  `deposit` decimal(10, 2) NOT NULL COMMENT '押金',
  `contract_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '合同文件地址',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '状态：0待生效 1生效中 2已结束',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_lease_contract_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_lease_contract_landlord_id`(`landlord_id` ASC) USING BTREE,
  INDEX `idx_lease_contract_application_id`(`application_id` ASC) USING BTREE,
  INDEX `fk_lease_contract_house`(`house_id` ASC) USING BTREE,
  CONSTRAINT `fk_lease_contract_application` FOREIGN KEY (`application_id`) REFERENCES `rental_application` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_lease_contract_house` FOREIGN KEY (`house_id`) REFERENCES `house` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_lease_contract_landlord` FOREIGN KEY (`landlord_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_lease_contract_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租赁合同表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of lease_contract
-- ----------------------------
INSERT INTO `lease_contract` VALUES (2, 1, 3, 2, 7, '2026-03-16', '2027-03-15', 3500.00, 3500.00, NULL, 1, '2026-03-16 19:25:34', '2026-03-16 19:25:34');
INSERT INTO `lease_contract` VALUES (3, 2, 3, 2, 8, '2026-04-24', '2027-04-23', 5200.00, 5200.00, NULL, 3, '2026-04-24 21:42:08', '2026-04-24 21:42:08');

-- ----------------------------
-- Table structure for lease_order
-- ----------------------------
DROP TABLE IF EXISTS `lease_order`;
CREATE TABLE `lease_order`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_id` bigint(20) NOT NULL COMMENT '合同ID',
  `tenant_id` bigint(20) NOT NULL COMMENT '租客ID',
  `amount` decimal(10, 2) NOT NULL COMMENT '订单金额',
  `pay_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付方式',
  `pay_status` tinyint(4) NULL DEFAULT 0 COMMENT '支付状态：0未支付 1已支付',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_lease_order_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_lease_order_contract_id`(`contract_id` ASC) USING BTREE,
  CONSTRAINT `fk_lease_order_contract` FOREIGN KEY (`contract_id`) REFERENCES `lease_contract` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_lease_order_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租赁订单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of lease_order
-- ----------------------------
INSERT INTO `lease_order` VALUES (2, 2, 3, 7000.00, 'ALIPAY', 1, '2026-03-16 20:25:21', '2026-03-16 19:25:34', '2026-03-16 19:25:34');
INSERT INTO `lease_order` VALUES (3, 3, 3, 10400.00, NULL, 3, NULL, '2026-04-24 21:42:08', '2026-04-24 21:42:08');

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告内容',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '公告表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of notice
-- ----------------------------
INSERT INTO `notice` VALUES (5, '本月租金缴纳提醒', '请各位租客于每月5日前完成租金缴纳，如有问题请及时联系管理员。', 1, '2026-03-16 15:55:44', '2026-03-16 15:55:44');
INSERT INTO `notice` VALUES (6, 'token鉴权测试公告', '这是一条通过token发布的公告', 1, '2026-03-23 21:32:56', '2026-03-23 21:32:56');

-- ----------------------------
-- Table structure for recommend_eval_label
-- ----------------------------
DROP TABLE IF EXISTS `recommend_eval_label`;
CREATE TABLE `recommend_eval_label`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `query_id` bigint(20) NOT NULL COMMENT '推荐实验查询ID',
  `house_id` bigint(20) NOT NULL COMMENT '相关房源ID',
  `relevance_score` int(11) NULL DEFAULT 1 COMMENT '相关性评分：1相关，2较相关，3非常相关',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_query_id`(`query_id` ASC) USING BTREE,
  INDEX `idx_house_id`(`house_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 102 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '推荐实验人工标注表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of recommend_eval_label
-- ----------------------------
INSERT INTO `recommend_eval_label` VALUES (5, 6, 4, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (6, 6, 11, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (7, 6, 12, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (8, 6, 5, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (9, 7, 11, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (10, 7, 18, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (11, 7, 15, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (12, 8, 4, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (13, 8, 12, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (14, 8, 11, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (15, 9, 5, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (16, 9, 15, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (17, 9, 12, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (18, 10, 4, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (19, 10, 6, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (20, 10, 11, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (21, 10, 18, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (22, 11, 4, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (23, 11, 11, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (24, 11, 15, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (25, 12, 6, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (26, 12, 14, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (27, 12, 7, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (28, 13, 6, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (29, 13, 14, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (30, 13, 19, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (31, 14, 7, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (32, 14, 14, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (33, 14, 20, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (34, 15, 7, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (35, 15, 18, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (36, 15, 13, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (37, 15, 20, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (38, 16, 14, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (39, 16, 6, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (40, 16, 20, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (41, 17, 20, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (42, 17, 7, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (43, 17, 10, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (44, 18, 7, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (45, 18, 13, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (46, 18, 18, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (47, 18, 21, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (48, 19, 13, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (49, 19, 21, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (50, 19, 7, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (51, 20, 13, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (52, 20, 19, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (53, 20, 21, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (54, 21, 4, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (55, 21, 12, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (56, 21, 13, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (57, 22, 13, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (58, 22, 18, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (59, 22, 4, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (60, 23, 21, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (61, 23, 7, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (62, 23, 13, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (63, 24, 5, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (64, 24, 15, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (65, 24, 17, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (66, 25, 5, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (67, 25, 12, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (68, 25, 4, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (69, 26, 15, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (70, 26, 17, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (71, 26, 5, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (72, 27, 5, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (73, 27, 15, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (74, 27, 6, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (75, 28, 5, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (76, 28, 17, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (77, 28, 12, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (78, 29, 5, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (79, 29, 15, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (80, 29, 4, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (81, 30, 8, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (82, 30, 9, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (83, 30, 16, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (84, 30, 19, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (85, 31, 10, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (86, 31, 20, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (87, 31, 8, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (88, 31, 14, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (89, 32, 9, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (90, 32, 16, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (91, 32, 19, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (92, 33, 9, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (93, 33, 16, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (94, 33, 8, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (95, 34, 19, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (96, 34, 8, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (97, 34, 10, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (98, 35, 10, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (99, 35, 20, 3, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (100, 35, 8, 2, '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_label` VALUES (101, 35, 9, 2, '2026-05-01 16:02:06');

-- ----------------------------
-- Table structure for recommend_eval_query
-- ----------------------------
DROP TABLE IF EXISTS `recommend_eval_query`;
CREATE TABLE `recommend_eval_query`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `query_text` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '测试查询语句',
  `scene_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '场景类型，如预算、交通、学生、装修等',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_scene_type`(`scene_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '推荐实验查询表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of recommend_eval_query
-- ----------------------------
INSERT INTO `recommend_eval_query` VALUES (6, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', '预算学生', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (7, '预算不超过1000元，想找低预算合租主卧或者单间', '预算学生', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (8, '想找大学附近，适合一个人住的单间或一室房源', '预算学生', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (9, '想找1500元以内，环境安静，适合考研复习的房子', '预算学生', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (10, '学生租房，希望交通方便，价格不要太高', '预算学生', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (11, '预算不超过1200元，最好靠近学校或者大学', '预算学生', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (12, '想找交通方便，靠近公交站或者主路的房子', '交通', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (13, '希望房子靠近公交站，平时出行方便', '交通', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (14, '想找通勤方便，靠近主路或者车站的房子', '交通', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (15, '想找交通便利，同时房子最好是精装的公寓', '交通', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (16, '希望附近有公交站，周边生活方便', '交通', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (17, '想找靠近商圈，交通方便，购物也方便的房子', '交通', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (18, '想找精装修，可以直接入住的公寓', '装修', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (19, '希望房子装修好，能够拎包入住', '装修', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (20, '想找有电梯，精装，居住体验好一点的一室', '装修', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (21, '想找精装一室一厅，适合单人居住', '装修', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (22, '希望房子适合一个人住，装修不要太旧', '装修', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (23, '想找拎包入住的两室房源，装修新一点', '装修', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (24, '想找安静一点，适合考研复习的房子', '学习环境', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (25, '希望靠近学校，适合学习，环境不要太吵', '学习环境', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (26, '想找安静小区，晚上休息不受影响', '学习环境', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (27, '考研期间居住，希望房子安静，同时交通方便', '学习环境', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (28, '想找安静的一室房源，适合学习和休息', '学习环境', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (29, '希望学习环境好，房子最好精装，周边安静', '学习环境', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (30, '想找适合一家人住，生活方便，周边配套齐全的房子', '家庭居住', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (31, '想找两室一厅，靠近商圈，购物生活方便', '家庭居住', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (32, '想找面积大一点，生活配套好的房子', '家庭居住', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (33, '想找三室，适合家庭居住，生活方便', '家庭居住', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (34, '想找医院附近，有电梯，生活方便的两室', '家庭居住', '2026-05-01 16:02:06');
INSERT INTO `recommend_eval_query` VALUES (35, '想找购物方便，靠近商圈，适合一家人住的房子', '家庭居住', '2026-05-01 16:02:06');

-- ----------------------------
-- Table structure for recommend_record
-- ----------------------------
DROP TABLE IF EXISTS `recommend_record`;
CREATE TABLE `recommend_record`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '用户ID，可为空',
  `query_text` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户输入的租房需求',
  `model_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '推荐模型类型',
  `result_house_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '推荐房源ID列表，逗号分隔',
  `response_time_ms` int(11) NULL DEFAULT NULL COMMENT '接口响应时间，单位毫秒',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_model_type`(`model_type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 894 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能推荐记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of recommend_record
-- ----------------------------
INSERT INTO `recommend_record` VALUES (1, NULL, '我想找精装公寓，交通方便，可以直接入住', 'RULE', '3,2', 698, '2026-05-01 15:18:07');
INSERT INTO `recommend_record` VALUES (2, NULL, '我想找一套1500元以内，适合学生住的房子', 'RULE', '3', 20, '2026-05-01 15:40:41');
INSERT INTO `recommend_record` VALUES (3, NULL, '我想找交通方便，靠近公交站或者主路的房子', 'RULE', '3,2', 4, '2026-05-01 15:40:41');
INSERT INTO `recommend_record` VALUES (4, NULL, '我想找精装修，可以直接入住的公寓', 'RULE', '3', 4, '2026-05-01 15:40:41');
INSERT INTO `recommend_record` VALUES (5, NULL, '我想找安静一点，适合考研复习的房子', 'RULE', '3', 3, '2026-05-01 15:40:41');
INSERT INTO `recommend_record` VALUES (6, NULL, '我想找面积大一点，适合一家人住的房子', 'RULE', '3,2', 3, '2026-05-01 15:40:41');
INSERT INTO `recommend_record` VALUES (7, NULL, '我想找一套1500元以内，适合学生住的房子', 'RULE', '3', 3, '2026-05-01 15:51:34');
INSERT INTO `recommend_record` VALUES (8, NULL, '我想找交通方便，靠近公交站或者主路的房子', 'RULE', '3,2', 3, '2026-05-01 15:51:34');
INSERT INTO `recommend_record` VALUES (9, NULL, '我想找精装修，可以直接入住的公寓', 'RULE', '3', 3, '2026-05-01 15:51:34');
INSERT INTO `recommend_record` VALUES (10, NULL, '我想找安静一点，适合考研复习的房子', 'RULE', '3', 8, '2026-05-01 15:51:34');
INSERT INTO `recommend_record` VALUES (11, NULL, '我想找面积大一点，适合一家人住的房子', 'RULE', '3,2', 5, '2026-05-01 15:51:34');
INSERT INTO `recommend_record` VALUES (12, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'RULE', '12,11,5,4,3', 2, '2026-05-01 16:03:00');
INSERT INTO `recommend_record` VALUES (13, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'RULE', '18,11', 2, '2026-05-01 16:03:00');
INSERT INTO `recommend_record` VALUES (14, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'RULE', '12,11,5,4,3', 4, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (15, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'RULE', '11,5,12,4,3', 3, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (16, NULL, '学生租房，希望交通方便，价格不要太高', 'RULE', '12,11,4,3,20', 4, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (17, NULL, '预算不超过1200元，最好靠近学校或者大学', 'RULE', '11,4,18,15', 3, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (18, NULL, '想找交通方便，靠近公交站或者主路的房子', 'RULE', '20,19,18,14,12', 3, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (19, NULL, '希望房子靠近公交站，平时出行方便', 'RULE', '20,19,18,14,12', 5, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (20, NULL, '想找通勤方便，靠近主路或者车站的房子', 'RULE', '20,19,18,14,12', 4, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (21, NULL, '想找交通便利，同时房子最好是精装的公寓', 'RULE', '18,12,7,4,3', 4, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (22, NULL, '希望附近有公交站，周边生活方便', 'RULE', '20,19,6,18,14', 3, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (23, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'RULE', '20,19,6,18,14', 5, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (24, NULL, '想找精装修，可以直接入住的公寓', 'RULE', '21,18,13,12,10', 4, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (25, NULL, '希望房子装修好，能够拎包入住', 'RULE', '21,18,13,12,10', 4, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (26, NULL, '想找有电梯，精装，居住体验好一点的一室', 'RULE', '21,18,13,12,10', 4, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (27, NULL, '想找精装一室一厅，适合单人居住', 'RULE', '18,13,12,7,4', 4, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (28, NULL, '希望房子适合一个人住，装修不要太旧', 'RULE', '18,13,12,7,4', 4, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (29, NULL, '想找拎包入住的两室房源，装修新一点', 'RULE', '21,18,13,12,10', 4, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (30, NULL, '想找安静一点，适合考研复习的房子', 'RULE', '11,5,12,9,4', 4, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (31, NULL, '希望靠近学校，适合学习，环境不要太吵', 'RULE', '11,5,12,9,4', 2, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (32, NULL, '想找安静小区，晚上休息不受影响', 'RULE', '17,15,11,5', 3, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (33, NULL, '考研期间居住，希望房子安静，同时交通方便', 'RULE', '11,12,4,3,5', 3, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (34, NULL, '想找安静的一室房源，适合学习和休息', 'RULE', '17,15,11,5', 3, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (35, NULL, '希望学习环境好，房子最好精装，周边安静', 'RULE', '21,18,13,12,10', 4, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (36, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'RULE', '21,20,19,16,10', 2, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (37, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'RULE', '21,20,19,16,10', 3, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (38, NULL, '想找面积大一点，生活配套好的房子', 'RULE', '21,20,19,16,10', 2, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (39, NULL, '想找三室，适合家庭居住，生活方便', 'RULE', '21,20,19,16,10', 4, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (40, NULL, '想找医院附近，有电梯，生活方便的两室', 'RULE', '21,20,19,16,10', 3, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (41, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'RULE', '21,20,19,16,10', 4, '2026-05-01 16:03:01');
INSERT INTO `recommend_record` VALUES (42, NULL, '我想找精装修，可以直接入住的公寓', 'TFIDF', '3,21,13,4,7', 86, '2026-05-01 16:24:09');
INSERT INTO `recommend_record` VALUES (43, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'RULE', '12,11,5,4,3', 5, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (44, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'RULE', '18,11', 4, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (45, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'RULE', '12,11,5,4,3', 7, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (46, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'RULE', '11,5,12,4,3', 4, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (47, NULL, '学生租房，希望交通方便，价格不要太高', 'RULE', '12,11,4,3,20', 6, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (48, NULL, '预算不超过1200元，最好靠近学校或者大学', 'RULE', '11,4,18,15', 4, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (49, NULL, '想找交通方便，靠近公交站或者主路的房子', 'RULE', '20,19,18,14,12', 4, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (50, NULL, '希望房子靠近公交站，平时出行方便', 'RULE', '20,19,18,14,12', 3, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (51, NULL, '想找通勤方便，靠近主路或者车站的房子', 'RULE', '20,19,18,14,12', 4, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (52, NULL, '想找交通便利，同时房子最好是精装的公寓', 'RULE', '18,12,7,4,3', 4, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (53, NULL, '希望附近有公交站，周边生活方便', 'RULE', '20,19,6,18,14', 3, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (54, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'RULE', '20,19,6,18,14', 2, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (55, NULL, '想找精装修，可以直接入住的公寓', 'RULE', '21,18,13,12,10', 2, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (56, NULL, '希望房子装修好，能够拎包入住', 'RULE', '21,18,13,12,10', 3, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (57, NULL, '想找有电梯，精装，居住体验好一点的一室', 'RULE', '21,18,13,12,10', 3, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (58, NULL, '想找精装一室一厅，适合单人居住', 'RULE', '18,13,12,7,4', 3, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (59, NULL, '希望房子适合一个人住，装修不要太旧', 'RULE', '18,13,12,7,4', 2, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (60, NULL, '想找拎包入住的两室房源，装修新一点', 'RULE', '21,18,13,12,10', 2, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (61, NULL, '想找安静一点，适合考研复习的房子', 'RULE', '11,5,12,9,4', 3, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (62, NULL, '希望靠近学校，适合学习，环境不要太吵', 'RULE', '11,5,12,9,4', 2, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (63, NULL, '想找安静小区，晚上休息不受影响', 'RULE', '17,15,11,5', 1, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (64, NULL, '考研期间居住，希望房子安静，同时交通方便', 'RULE', '11,12,4,3,5', 2, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (65, NULL, '想找安静的一室房源，适合学习和休息', 'RULE', '17,15,11,5', 3, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (66, NULL, '希望学习环境好，房子最好精装，周边安静', 'RULE', '21,18,13,12,10', 5, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (67, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'RULE', '21,20,19,16,10', 3, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (68, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'RULE', '21,20,19,16,10', 3, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (69, NULL, '想找面积大一点，生活配套好的房子', 'RULE', '21,20,19,16,10', 3, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (70, NULL, '想找三室，适合家庭居住，生活方便', 'RULE', '21,20,19,16,10', 4, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (71, NULL, '想找医院附近，有电梯，生活方便的两室', 'RULE', '21,20,19,16,10', 4, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (72, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'RULE', '21,20,19,16,10', 4, '2026-05-01 16:25:14');
INSERT INTO `recommend_record` VALUES (73, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'TFIDF', '3,12,4,11,15', 3, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (74, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'TFIDF', '11,18', 4, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (75, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'TFIDF', '3,4,18,6,11', 8, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (76, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'TFIDF', '5,15,18,3,12', 3, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (77, NULL, '学生租房，希望交通方便，价格不要太高', 'TFIDF', '3,4,11,2,14', 8, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (78, NULL, '预算不超过1200元，最好靠近学校或者大学', 'TFIDF', '11,4', 4, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (79, NULL, '想找交通方便，靠近公交站或者主路的房子', 'TFIDF', '14,7,6,3,4', 8, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (80, NULL, '希望房子靠近公交站，平时出行方便', 'TFIDF', '6,14,19,7,17', 5, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (81, NULL, '想找通勤方便，靠近主路或者车站的房子', 'TFIDF', '14,7,3,2,20', 6, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (82, NULL, '想找交通便利，同时房子最好是精装的公寓', 'TFIDF', '7,3,18,14,20', 6, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (83, NULL, '希望附近有公交站，周边生活方便', 'TFIDF', '6,19,8,21,14', 4, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (84, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'TFIDF', '20,10,2,14,3', 5, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (85, NULL, '想找精装修，可以直接入住的公寓', 'TFIDF', '3,21,13,4,7', 6, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (86, NULL, '希望房子装修好，能够拎包入住', 'TFIDF', '21,13,3,4,7', 5, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (87, NULL, '想找有电梯，精装，居住体验好一点的一室', 'TFIDF', '13,3,19,7,17', 5, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (88, NULL, '想找精装一室一厅，适合单人居住', 'TFIDF', '13,3,17,12,18', 5, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (89, NULL, '希望房子适合一个人住，装修不要太旧', 'TFIDF', '21,13,3,4,8', 5, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (90, NULL, '想找拎包入住的两室房源，装修新一点', 'TFIDF', '21,13,3,4,7', 4, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (91, NULL, '想找安静一点，适合考研复习的房子', 'TFIDF', '5,15,17,14,13', 4, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (92, NULL, '希望靠近学校，适合学习，环境不要太吵', 'TFIDF', '3,12,5,4,15', 7, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (93, NULL, '想找安静小区，晚上休息不受影响', 'TFIDF', '15,17,5', 6, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (94, NULL, '考研期间居住，希望房子安静，同时交通方便', 'TFIDF', '5,15,17,14,2', 6, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (95, NULL, '想找安静的一室房源，适合学习和休息', 'TFIDF', '5,15,17,12,3', 7, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (96, NULL, '希望学习环境好，房子最好精装，周边安静', 'TFIDF', '5,15,17,4,21', 6, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (97, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'TFIDF', '8,19,16,21,6', 5, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (98, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'TFIDF', '10,20,8,21,19', 4, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (99, NULL, '想找面积大一点，生活配套好的房子', 'TFIDF', '16,9,6,10,8', 5, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (100, NULL, '想找三室，适合家庭居住，生活方便', 'TFIDF', '9,16,19,8,21', 5, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (101, NULL, '想找医院附近，有电梯，生活方便的两室', 'TFIDF', '19,21,8,13,10', 6, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (102, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'TFIDF', '20,10,8,3,14', 6, '2026-05-01 16:25:32');
INSERT INTO `recommend_record` VALUES (103, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'TFIDF', '3,12,4,11,15,5,18,6', 4, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (104, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'TFIDF', '11,18', 1, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (105, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'TFIDF', '3,4,18,6,11,13,8,17,12,5', 3, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (106, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'TFIDF', '5,15,18,3,12,6,4,11', 2, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (107, NULL, '学生租房，希望交通方便，价格不要太高', 'TFIDF', '3,4,11,2,14,6,18,12,20,7', 5, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (108, NULL, '预算不超过1200元，最好靠近学校或者大学', 'TFIDF', '11,4', 5, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (109, NULL, '想找交通方便，靠近公交站或者主路的房子', 'TFIDF', '14,7,6,3,4,2,19,18,17,11', 2, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (110, NULL, '希望房子靠近公交站，平时出行方便', 'TFIDF', '6,14,19,7,17,4,12,2,3,20', 3, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (111, NULL, '想找通勤方便，靠近主路或者车站的房子', 'TFIDF', '14,7,3,2,20,4,10,19,11,17', 4, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (112, NULL, '想找交通便利，同时房子最好是精装的公寓', 'TFIDF', '7,3,18,14,20,4,21,13,12,10', 4, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (113, NULL, '希望附近有公交站，周边生活方便', 'TFIDF', '6,19,8,21,14,7,16,12,4,10', 3, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (114, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'TFIDF', '20,10,2,14,3,4,6,18,11,7', 5, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (115, NULL, '想找精装修，可以直接入住的公寓', 'TFIDF', '3,21,13,4,7,18,12,10', 4, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (116, NULL, '希望房子装修好，能够拎包入住', 'TFIDF', '21,13,3,4,7', 4, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (117, NULL, '想找有电梯，精装，居住体验好一点的一室', 'TFIDF', '13,3,19,7,17,21,12,4,18,5', 4, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (118, NULL, '想找精装一室一厅，适合单人居住', 'TFIDF', '13,3,17,12,18,6,5,4,7,21', 3, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (119, NULL, '希望房子适合一个人住，装修不要太旧', 'TFIDF', '21,13,3,4,8,18,17,14,6,20', 4, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (120, NULL, '想找拎包入住的两室房源，装修新一点', 'TFIDF', '21,13,3,4,7,2,8,14,10,19', 5, '2026-05-01 16:38:09');
INSERT INTO `recommend_record` VALUES (121, NULL, '想找安静一点，适合考研复习的房子', 'TFIDF', '5,15,17,14,13,8,3,20,18,6', 4, '2026-05-01 16:38:10');
INSERT INTO `recommend_record` VALUES (122, NULL, '希望靠近学校，适合学习，环境不要太吵', 'TFIDF', '3,12,5,4,15,9,11,17,14,20', 4, '2026-05-01 16:38:10');
INSERT INTO `recommend_record` VALUES (123, NULL, '想找安静小区，晚上休息不受影响', 'TFIDF', '15,17,5', 4, '2026-05-01 16:38:10');
INSERT INTO `recommend_record` VALUES (124, NULL, '考研期间居住，希望房子安静，同时交通方便', 'TFIDF', '5,15,17,14,2,3,6,18,4,11', 4, '2026-05-01 16:38:10');
INSERT INTO `recommend_record` VALUES (125, NULL, '想找安静的一室房源，适合学习和休息', 'TFIDF', '5,15,17,12,3,13,7,4,11,18', 5, '2026-05-01 16:38:10');
INSERT INTO `recommend_record` VALUES (126, NULL, '希望学习环境好，房子最好精装，周边安静', 'TFIDF', '5,15,17,4,21,13,3,7,18,8', 3, '2026-05-01 16:38:10');
INSERT INTO `recommend_record` VALUES (127, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'TFIDF', '8,19,16,21,6,9,10,12,4,11', 4, '2026-05-01 16:38:10');
INSERT INTO `recommend_record` VALUES (128, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'TFIDF', '10,20,8,21,19,2,14,5,3,17', 5, '2026-05-01 16:38:10');
INSERT INTO `recommend_record` VALUES (129, NULL, '想找面积大一点，生活配套好的房子', 'TFIDF', '16,9,6,10,8,19,21,15,11', 4, '2026-05-01 16:38:10');
INSERT INTO `recommend_record` VALUES (130, NULL, '想找三室，适合家庭居住，生活方便', 'TFIDF', '9,16,19,8,21,10,20,6,14,11', 4, '2026-05-01 16:38:10');
INSERT INTO `recommend_record` VALUES (131, NULL, '想找医院附近，有电梯，生活方便的两室', 'TFIDF', '19,21,8,13,10,2,14,20,6,11', 3, '2026-05-01 16:38:10');
INSERT INTO `recommend_record` VALUES (132, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'TFIDF', '20,10,8,3,14,4,19,2,11,17', 5, '2026-05-01 16:38:10');
INSERT INTO `recommend_record` VALUES (133, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'RULE', '12,11,5,4,3', 10, '2026-05-01 19:41:15');
INSERT INTO `recommend_record` VALUES (134, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'RULE', '18,11', 2, '2026-05-01 19:41:15');
INSERT INTO `recommend_record` VALUES (135, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'RULE', '12,11,5,4,3', 4, '2026-05-01 19:41:15');
INSERT INTO `recommend_record` VALUES (136, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'RULE', '11,5,12,4,3', 3, '2026-05-01 19:41:15');
INSERT INTO `recommend_record` VALUES (137, NULL, '学生租房，希望交通方便，价格不要太高', 'RULE', '12,11,4,3,20', 4, '2026-05-01 19:41:15');
INSERT INTO `recommend_record` VALUES (138, NULL, '预算不超过1200元，最好靠近学校或者大学', 'RULE', '11,4,18,15', 3, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (139, NULL, '想找交通方便，靠近公交站或者主路的房子', 'RULE', '20,19,18,14,12', 3, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (140, NULL, '希望房子靠近公交站，平时出行方便', 'RULE', '20,19,18,14,12', 4, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (141, NULL, '想找通勤方便，靠近主路或者车站的房子', 'RULE', '20,19,18,14,12', 4, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (142, NULL, '想找交通便利，同时房子最好是精装的公寓', 'RULE', '18,12,7,4,3', 3, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (143, NULL, '希望附近有公交站，周边生活方便', 'RULE', '20,19,6,18,14', 4, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (144, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'RULE', '20,19,6,18,14', 3, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (145, NULL, '想找精装修，可以直接入住的公寓', 'RULE', '21,18,13,12,10', 3, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (146, NULL, '希望房子装修好，能够拎包入住', 'RULE', '21,18,13,12,10', 1, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (147, NULL, '想找有电梯，精装，居住体验好一点的一室', 'RULE', '21,18,13,12,10', 2, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (148, NULL, '想找精装一室一厅，适合单人居住', 'RULE', '18,13,12,7,4', 3, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (149, NULL, '希望房子适合一个人住，装修不要太旧', 'RULE', '18,13,12,7,4', 4, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (150, NULL, '想找拎包入住的两室房源，装修新一点', 'RULE', '21,18,13,12,10', 2, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (151, NULL, '想找安静一点，适合考研复习的房子', 'RULE', '11,5,12,9,4', 2, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (152, NULL, '希望靠近学校，适合学习，环境不要太吵', 'RULE', '11,5,12,9,4', 2, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (153, NULL, '想找安静小区，晚上休息不受影响', 'RULE', '17,15,11,5', 4, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (154, NULL, '考研期间居住，希望房子安静，同时交通方便', 'RULE', '11,12,4,3,5', 2, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (155, NULL, '想找安静的一室房源，适合学习和休息', 'RULE', '17,15,11,5', 3, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (156, NULL, '希望学习环境好，房子最好精装，周边安静', 'RULE', '21,18,13,12,10', 2, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (157, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'RULE', '21,20,19,16,10', 3, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (158, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'RULE', '21,20,19,16,10', 3, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (159, NULL, '想找面积大一点，生活配套好的房子', 'RULE', '21,20,19,16,10', 4, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (160, NULL, '想找三室，适合家庭居住，生活方便', 'RULE', '21,20,19,16,10', 3, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (161, NULL, '想找医院附近，有电梯，生活方便的两室', 'RULE', '21,20,19,16,10', 3, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (162, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'RULE', '21,20,19,16,10', 2, '2026-05-01 19:41:16');
INSERT INTO `recommend_record` VALUES (163, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'TFIDF', '3,12,4,11,15,5,18,6', 9, '2026-05-01 19:41:42');
INSERT INTO `recommend_record` VALUES (164, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'TFIDF', '11,18', 3, '2026-05-01 19:41:42');
INSERT INTO `recommend_record` VALUES (165, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'TFIDF', '3,4,18,6,11,13,8,17,12,5', 8, '2026-05-01 19:41:42');
INSERT INTO `recommend_record` VALUES (166, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'TFIDF', '5,15,18,3,12,6,4,11', 4, '2026-05-01 19:41:42');
INSERT INTO `recommend_record` VALUES (167, NULL, '学生租房，希望交通方便，价格不要太高', 'TFIDF', '3,4,11,2,14,6,18,12,20,7', 6, '2026-05-01 19:41:42');
INSERT INTO `recommend_record` VALUES (168, NULL, '预算不超过1200元，最好靠近学校或者大学', 'TFIDF', '11,4', 3, '2026-05-01 19:41:42');
INSERT INTO `recommend_record` VALUES (169, NULL, '我想找安静一点，适合考研复习的房子', 'EMBEDDING', '5,15,17,8,4', 1592, '2026-05-02 09:05:04');
INSERT INTO `recommend_record` VALUES (170, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'RULE', '12,11,5,4,3', 7, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (171, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'RULE', '18,11', 5, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (172, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'RULE', '12,11,5,4,3', 9, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (173, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'RULE', '11,5,12,4,3', 4, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (174, NULL, '学生租房，希望交通方便，价格不要太高', 'RULE', '12,11,4,3,20', 5, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (175, NULL, '预算不超过1200元，最好靠近学校或者大学', 'RULE', '11,4,18,15', 4, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (176, NULL, '想找交通方便，靠近公交站或者主路的房子', 'RULE', '20,19,18,14,12', 2, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (177, NULL, '希望房子靠近公交站，平时出行方便', 'RULE', '20,19,18,14,12', 5, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (178, NULL, '想找通勤方便，靠近主路或者车站的房子', 'RULE', '20,19,18,14,12', 3, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (179, NULL, '想找交通便利，同时房子最好是精装的公寓', 'RULE', '18,12,7,4,3', 3, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (180, NULL, '希望附近有公交站，周边生活方便', 'RULE', '20,19,6,18,14', 3, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (181, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'RULE', '20,19,6,18,14', 4, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (182, NULL, '想找精装修，可以直接入住的公寓', 'RULE', '21,18,13,12,10', 3, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (183, NULL, '希望房子装修好，能够拎包入住', 'RULE', '21,18,13,12,10', 4, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (184, NULL, '想找有电梯，精装，居住体验好一点的一室', 'RULE', '21,18,13,12,10', 3, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (185, NULL, '想找精装一室一厅，适合单人居住', 'RULE', '18,13,12,7,4', 4, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (186, NULL, '希望房子适合一个人住，装修不要太旧', 'RULE', '18,13,12,7,4', 4, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (187, NULL, '想找拎包入住的两室房源，装修新一点', 'RULE', '21,18,13,12,10', 5, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (188, NULL, '想找安静一点，适合考研复习的房子', 'RULE', '11,5,12,9,4', 3, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (189, NULL, '希望靠近学校，适合学习，环境不要太吵', 'RULE', '11,5,12,9,4', 2, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (190, NULL, '想找安静小区，晚上休息不受影响', 'RULE', '17,15,11,5', 3, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (191, NULL, '考研期间居住，希望房子安静，同时交通方便', 'RULE', '11,12,4,3,5', 3, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (192, NULL, '想找安静的一室房源，适合学习和休息', 'RULE', '17,15,11,5', 4, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (193, NULL, '希望学习环境好，房子最好精装，周边安静', 'RULE', '21,18,13,12,10', 2, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (194, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'RULE', '21,20,19,16,10', 3, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (195, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'RULE', '21,20,19,16,10', 3, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (196, NULL, '想找面积大一点，生活配套好的房子', 'RULE', '21,20,19,16,10', 2, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (197, NULL, '想找三室，适合家庭居住，生活方便', 'RULE', '21,20,19,16,10', 2, '2026-05-02 09:05:25');
INSERT INTO `recommend_record` VALUES (198, NULL, '想找医院附近，有电梯，生活方便的两室', 'RULE', '21,20,19,16,10', 3, '2026-05-02 09:05:26');
INSERT INTO `recommend_record` VALUES (199, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'RULE', '21,20,19,16,10', 3, '2026-05-02 09:05:26');
INSERT INTO `recommend_record` VALUES (200, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'EMBEDDING', '3,4,11,5,12', 439, '2026-05-02 09:05:30');
INSERT INTO `recommend_record` VALUES (201, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'EMBEDDING', '11,18', 163, '2026-05-02 09:05:31');
INSERT INTO `recommend_record` VALUES (202, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'EMBEDDING', '12,4,11,5,3', 775, '2026-05-02 09:05:31');
INSERT INTO `recommend_record` VALUES (203, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'EMBEDDING', '5,15,11,4,3', 375, '2026-05-02 09:05:32');
INSERT INTO `recommend_record` VALUES (204, NULL, '学生租房，希望交通方便，价格不要太高', 'EMBEDDING', '11,4,12,18,3', 885, '2026-05-02 09:05:33');
INSERT INTO `recommend_record` VALUES (205, NULL, '预算不超过1200元，最好靠近学校或者大学', 'EMBEDDING', '4,11,18,15', 301, '2026-05-02 09:05:33');
INSERT INTO `recommend_record` VALUES (206, NULL, '想找交通方便，靠近公交站或者主路的房子', 'EMBEDDING', '7,12,4,6,20', 837, '2026-05-02 09:05:34');
INSERT INTO `recommend_record` VALUES (207, NULL, '希望房子靠近公交站，平时出行方便', 'EMBEDDING', '7,20,6,14,2', 807, '2026-05-02 09:05:35');
INSERT INTO `recommend_record` VALUES (208, NULL, '想找通勤方便，靠近主路或者车站的房子', 'EMBEDDING', '7,4,12,20,11', 826, '2026-05-02 09:05:35');
INSERT INTO `recommend_record` VALUES (209, NULL, '想找交通便利，同时房子最好是精装的公寓', 'EMBEDDING', '3,7,4,8,21', 834, '2026-05-02 09:05:36');
INSERT INTO `recommend_record` VALUES (210, NULL, '希望附近有公交站，周边生活方便', 'EMBEDDING', '6,7,14,20,2', 832, '2026-05-02 09:05:37');
INSERT INTO `recommend_record` VALUES (211, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'EMBEDDING', '20,10,9,4,14', 825, '2026-05-02 09:05:38');
INSERT INTO `recommend_record` VALUES (212, NULL, '想找精装修，可以直接入住的公寓', 'EMBEDDING', '3,21,4,7,16', 810, '2026-05-02 09:05:39');
INSERT INTO `recommend_record` VALUES (213, NULL, '希望房子装修好，能够拎包入住', 'EMBEDDING', '21,3,4,13,8', 801, '2026-05-02 09:05:40');
INSERT INTO `recommend_record` VALUES (214, NULL, '想找有电梯，精装，居住体验好一点的一室', 'EMBEDDING', '13,3,8,16,21', 827, '2026-05-02 09:05:40');
INSERT INTO `recommend_record` VALUES (215, NULL, '想找精装一室一厅，适合单人居住', 'EMBEDDING', '8,3,4,16,21', 706, '2026-05-02 09:05:41');
INSERT INTO `recommend_record` VALUES (216, NULL, '希望房子适合一个人住，装修不要太旧', 'EMBEDDING', '8,21,3,4,17', 714, '2026-05-02 09:05:42');
INSERT INTO `recommend_record` VALUES (217, NULL, '想找拎包入住的两室房源，装修新一点', 'EMBEDDING', '21,8,3,4,13', 718, '2026-05-02 09:05:43');
INSERT INTO `recommend_record` VALUES (218, NULL, '想找安静一点，适合考研复习的房子', 'EMBEDDING', '5,15,17,12,4', 737, '2026-05-02 09:05:43');
INSERT INTO `recommend_record` VALUES (219, NULL, '希望靠近学校，适合学习，环境不要太吵', 'EMBEDDING', '4,15,5,17,11', 704, '2026-05-02 09:05:44');
INSERT INTO `recommend_record` VALUES (220, NULL, '想找安静小区，晚上休息不受影响', 'EMBEDDING', '15,17,5,8,11', 722, '2026-05-02 09:05:45');
INSERT INTO `recommend_record` VALUES (221, NULL, '考研期间居住，希望房子安静，同时交通方便', 'EMBEDDING', '5,15,17,11,12', 720, '2026-05-02 09:05:46');
INSERT INTO `recommend_record` VALUES (222, NULL, '想找安静的一室房源，适合学习和休息', 'EMBEDDING', '15,5,17,11,12', 707, '2026-05-02 09:05:46');
INSERT INTO `recommend_record` VALUES (223, NULL, '希望学习环境好，房子最好精装，周边安静', 'EMBEDDING', '5,3,4,15,12', 695, '2026-05-02 09:05:47');
INSERT INTO `recommend_record` VALUES (224, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'EMBEDDING', '8,20,9,12,17', 722, '2026-05-02 09:05:48');
INSERT INTO `recommend_record` VALUES (225, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'EMBEDDING', '20,10,8,9,19', 745, '2026-05-02 09:05:49');
INSERT INTO `recommend_record` VALUES (226, NULL, '想找面积大一点，生活配套好的房子', 'EMBEDDING', '8,3,4,20,9', 763, '2026-05-02 09:05:49');
INSERT INTO `recommend_record` VALUES (227, NULL, '想找三室，适合家庭居住，生活方便', 'EMBEDDING', '9,16,15,8,11', 778, '2026-05-02 09:05:50');
INSERT INTO `recommend_record` VALUES (228, NULL, '想找医院附近，有电梯，生活方便的两室', 'EMBEDDING', '19,20,14,10,8', 782, '2026-05-02 09:05:51');
INSERT INTO `recommend_record` VALUES (229, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'EMBEDDING', '20,10,9,8,12', 813, '2026-05-02 09:05:52');
INSERT INTO `recommend_record` VALUES (230, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'EMBEDDING', '3,4,11,5,12,18,15,6', 414, '2026-05-02 09:05:56');
INSERT INTO `recommend_record` VALUES (231, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'EMBEDDING', '11,18', 169, '2026-05-02 09:05:56');
INSERT INTO `recommend_record` VALUES (232, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'EMBEDDING', '12,4,11,5,3,15,17,18,8,9', 881, '2026-05-02 09:05:57');
INSERT INTO `recommend_record` VALUES (233, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'EMBEDDING', '5,15,11,4,3,18,12,6', 368, '2026-05-02 09:05:58');
INSERT INTO `recommend_record` VALUES (234, NULL, '学生租房，希望交通方便，价格不要太高', 'EMBEDDING', '11,4,12,18,3,7,20,5,9,15', 804, '2026-05-02 09:05:59');
INSERT INTO `recommend_record` VALUES (235, NULL, '预算不超过1200元，最好靠近学校或者大学', 'EMBEDDING', '4,11,18,15', 221, '2026-05-02 09:05:59');
INSERT INTO `recommend_record` VALUES (236, NULL, '想找交通方便，靠近公交站或者主路的房子', 'EMBEDDING', '7,12,4,6,20,14,11,10,18,2', 777, '2026-05-02 09:06:00');
INSERT INTO `recommend_record` VALUES (237, NULL, '希望房子靠近公交站，平时出行方便', 'EMBEDDING', '7,20,6,14,2,10,4,12,11,17', 824, '2026-05-02 09:06:00');
INSERT INTO `recommend_record` VALUES (238, NULL, '想找通勤方便，靠近主路或者车站的房子', 'EMBEDDING', '7,4,12,20,11,14,6,10,18,9', 772, '2026-05-02 09:06:01');
INSERT INTO `recommend_record` VALUES (239, NULL, '想找交通便利，同时房子最好是精装的公寓', 'EMBEDDING', '3,7,4,8,21,12,20,16,10,18', 796, '2026-05-02 09:06:02');
INSERT INTO `recommend_record` VALUES (240, NULL, '希望附近有公交站，周边生活方便', 'EMBEDDING', '6,7,14,20,2,10,17,19,4,11', 811, '2026-05-02 09:06:03');
INSERT INTO `recommend_record` VALUES (241, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'EMBEDDING', '20,10,9,4,14,6,12,7,11,3', 789, '2026-05-02 09:06:04');
INSERT INTO `recommend_record` VALUES (242, NULL, '想找精装修，可以直接入住的公寓', 'EMBEDDING', '3,21,4,7,16,18,8,13,12,17', 788, '2026-05-02 09:06:04');
INSERT INTO `recommend_record` VALUES (243, NULL, '希望房子装修好，能够拎包入住', 'EMBEDDING', '21,3,4,13,8,7,16,9,12,17', 822, '2026-05-02 09:06:05');
INSERT INTO `recommend_record` VALUES (244, NULL, '想找有电梯，精装，居住体验好一点的一室', 'EMBEDDING', '13,3,8,16,21,4,19,7,20,17', 789, '2026-05-02 09:06:06');
INSERT INTO `recommend_record` VALUES (245, NULL, '想找精装一室一厅，适合单人居住', 'EMBEDDING', '8,3,4,16,21,13,17,18,9,20', 766, '2026-05-02 09:06:07');
INSERT INTO `recommend_record` VALUES (246, NULL, '希望房子适合一个人住，装修不要太旧', 'EMBEDDING', '8,21,3,4,17,16,13,15,12,18', 780, '2026-05-02 09:06:08');
INSERT INTO `recommend_record` VALUES (247, NULL, '想找拎包入住的两室房源，装修新一点', 'EMBEDDING', '21,8,3,4,13,20,10,16,19,12', 763, '2026-05-02 09:06:08');
INSERT INTO `recommend_record` VALUES (248, NULL, '想找安静一点，适合考研复习的房子', 'EMBEDDING', '5,15,17,12,4,8,11,9,20,16', 732, '2026-05-02 09:06:09');
INSERT INTO `recommend_record` VALUES (249, NULL, '希望靠近学校，适合学习，环境不要太吵', 'EMBEDDING', '4,15,5,17,11,12,3,8,9,16', 773, '2026-05-02 09:06:10');
INSERT INTO `recommend_record` VALUES (250, NULL, '想找安静小区，晚上休息不受影响', 'EMBEDDING', '15,17,5,8,11,10,20,4,12,18', 802, '2026-05-02 09:06:11');
INSERT INTO `recommend_record` VALUES (251, NULL, '考研期间居住，希望房子安静，同时交通方便', 'EMBEDDING', '5,15,17,11,12,4,20,8,3,9', 735, '2026-05-02 09:06:12');
INSERT INTO `recommend_record` VALUES (252, NULL, '想找安静的一室房源，适合学习和休息', 'EMBEDDING', '15,5,17,11,12,8,4,3,10,18', 758, '2026-05-02 09:06:12');
INSERT INTO `recommend_record` VALUES (253, NULL, '希望学习环境好，房子最好精装，周边安静', 'EMBEDDING', '5,3,4,15,12,17,8,9,11,21', 777, '2026-05-02 09:06:13');
INSERT INTO `recommend_record` VALUES (254, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'EMBEDDING', '8,20,9,12,17,10,16,3,4,11', 798, '2026-05-02 09:06:14');
INSERT INTO `recommend_record` VALUES (255, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'EMBEDDING', '20,10,8,9,19,14,16,21,3,12', 763, '2026-05-02 09:06:15');
INSERT INTO `recommend_record` VALUES (256, NULL, '想找面积大一点，生活配套好的房子', 'EMBEDDING', '8,3,4,20,9,16,21,10,17,12', 797, '2026-05-02 09:06:16');
INSERT INTO `recommend_record` VALUES (257, NULL, '想找三室，适合家庭居住，生活方便', 'EMBEDDING', '9,16,15,8,11,3,12,20,4,17', 789, '2026-05-02 09:06:16');
INSERT INTO `recommend_record` VALUES (258, NULL, '想找医院附近，有电梯，生活方便的两室', 'EMBEDDING', '19,20,14,10,8,12,13,6,4,2', 804, '2026-05-02 09:06:17');
INSERT INTO `recommend_record` VALUES (259, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'EMBEDDING', '20,10,9,8,12,4,11,16,3,17', 801, '2026-05-02 09:06:18');
INSERT INTO `recommend_record` VALUES (260, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'EMBEDDING', '3,4,11,5,12,18,15,6', 382, '2026-05-02 09:06:21');
INSERT INTO `recommend_record` VALUES (261, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'EMBEDDING', '11,18', 165, '2026-05-02 09:06:21');
INSERT INTO `recommend_record` VALUES (262, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'EMBEDDING', '12,4,11,5,3,15,17,18,8,9', 852, '2026-05-02 09:06:22');
INSERT INTO `recommend_record` VALUES (263, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'EMBEDDING', '5,15,11,4,3,18,12,6', 393, '2026-05-02 09:06:22');
INSERT INTO `recommend_record` VALUES (264, NULL, '学生租房，希望交通方便，价格不要太高', 'EMBEDDING', '11,4,12,18,3,7,20,5,9,15', 911, '2026-05-02 09:06:23');
INSERT INTO `recommend_record` VALUES (265, NULL, '预算不超过1200元，最好靠近学校或者大学', 'EMBEDDING', '4,11,18,15', 267, '2026-05-02 09:06:24');
INSERT INTO `recommend_record` VALUES (266, NULL, '想找交通方便，靠近公交站或者主路的房子', 'EMBEDDING', '7,12,4,6,20,14,11,10,18,2', 863, '2026-05-02 09:06:24');
INSERT INTO `recommend_record` VALUES (267, NULL, '希望房子靠近公交站，平时出行方便', 'EMBEDDING', '7,20,6,14,2,10,4,12,11,17', 858, '2026-05-02 09:06:25');
INSERT INTO `recommend_record` VALUES (268, NULL, '想找通勤方便，靠近主路或者车站的房子', 'EMBEDDING', '7,4,12,20,11,14,6,10,18,9', 836, '2026-05-02 09:06:26');
INSERT INTO `recommend_record` VALUES (269, NULL, '想找交通便利，同时房子最好是精装的公寓', 'EMBEDDING', '3,7,4,8,21,12,20,16,10,18', 839, '2026-05-02 09:06:27');
INSERT INTO `recommend_record` VALUES (270, NULL, '希望附近有公交站，周边生活方便', 'EMBEDDING', '6,7,14,20,2,10,17,19,4,11', 856, '2026-05-02 09:06:28');
INSERT INTO `recommend_record` VALUES (271, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'EMBEDDING', '20,10,9,4,14,6,12,7,11,3', 814, '2026-05-02 09:06:29');
INSERT INTO `recommend_record` VALUES (272, NULL, '想找精装修，可以直接入住的公寓', 'EMBEDDING', '3,21,4,7,16,18,8,13,12,17', 818, '2026-05-02 09:06:30');
INSERT INTO `recommend_record` VALUES (273, NULL, '希望房子装修好，能够拎包入住', 'EMBEDDING', '21,3,4,13,8,7,16,9,12,17', 885, '2026-05-02 09:06:30');
INSERT INTO `recommend_record` VALUES (274, NULL, '想找有电梯，精装，居住体验好一点的一室', 'EMBEDDING', '13,3,8,16,21,4,19,7,20,17', 857, '2026-05-02 09:06:31');
INSERT INTO `recommend_record` VALUES (275, NULL, '想找精装一室一厅，适合单人居住', 'EMBEDDING', '8,3,4,16,21,13,17,18,9,20', 835, '2026-05-02 09:06:32');
INSERT INTO `recommend_record` VALUES (276, NULL, '希望房子适合一个人住，装修不要太旧', 'EMBEDDING', '8,21,3,4,17,16,13,15,12,18', 851, '2026-05-02 09:06:33');
INSERT INTO `recommend_record` VALUES (277, NULL, '想找拎包入住的两室房源，装修新一点', 'EMBEDDING', '21,8,3,4,13,20,10,16,19,12', 842, '2026-05-02 09:06:34');
INSERT INTO `recommend_record` VALUES (278, NULL, '想找安静一点，适合考研复习的房子', 'EMBEDDING', '5,15,17,12,4,8,11,9,20,16', 841, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (279, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'TFIDF', '3,12,4,11,15,5,18,6', 8, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (280, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'TFIDF', '11,18', 4, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (281, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'TFIDF', '3,4,18,6,11,13,8,17,12,5', 10, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (282, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'TFIDF', '5,15,18,3,12,6,4,11', 6, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (283, NULL, '学生租房，希望交通方便，价格不要太高', 'TFIDF', '3,4,11,2,14,6,18,12,20,7', 11, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (284, NULL, '预算不超过1200元，最好靠近学校或者大学', 'TFIDF', '11,4', 5, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (285, NULL, '想找交通方便，靠近公交站或者主路的房子', 'TFIDF', '14,7,6,3,4,2,19,18,17,11', 9, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (286, NULL, '希望房子靠近公交站，平时出行方便', 'TFIDF', '6,14,19,7,17,4,12,2,3,20', 11, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (287, NULL, '想找通勤方便，靠近主路或者车站的房子', 'TFIDF', '14,7,3,2,20,4,10,19,11,17', 8, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (288, NULL, '想找交通便利，同时房子最好是精装的公寓', 'TFIDF', '7,3,18,14,20,4,21,13,12,10', 6, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (289, NULL, '希望附近有公交站，周边生活方便', 'TFIDF', '6,19,8,21,14,7,16,12,4,10', 6, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (290, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'TFIDF', '20,10,2,14,3,4,6,18,11,7', 7, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (291, NULL, '想找精装修，可以直接入住的公寓', 'TFIDF', '3,21,13,4,7,18,12,10', 7, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (292, NULL, '希望房子装修好，能够拎包入住', 'TFIDF', '21,13,3,4,7', 12, '2026-05-02 09:06:35');
INSERT INTO `recommend_record` VALUES (293, NULL, '想找有电梯，精装，居住体验好一点的一室', 'TFIDF', '13,3,19,7,17,21,12,4,18,5', 9, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (294, NULL, '想找精装一室一厅，适合单人居住', 'TFIDF', '13,3,17,12,18,6,5,4,7,21', 6, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (295, NULL, '希望房子适合一个人住，装修不要太旧', 'TFIDF', '21,13,3,4,8,18,17,14,6,20', 6, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (296, NULL, '想找拎包入住的两室房源，装修新一点', 'TFIDF', '21,13,3,4,7,2,8,14,10,19', 10, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (297, NULL, '想找安静一点，适合考研复习的房子', 'TFIDF', '5,15,17,14,13,8,3,20,18,6', 8, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (298, NULL, '希望靠近学校，适合学习，环境不要太吵', 'TFIDF', '3,12,5,4,15,9,11,17,14,20', 6, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (299, NULL, '想找安静小区，晚上休息不受影响', 'TFIDF', '15,17,5', 7, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (300, NULL, '考研期间居住，希望房子安静，同时交通方便', 'TFIDF', '5,15,17,14,2,3,6,18,4,11', 6, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (301, NULL, '想找安静的一室房源，适合学习和休息', 'TFIDF', '5,15,17,12,3,13,7,4,11,18', 7, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (302, NULL, '希望学习环境好，房子最好精装，周边安静', 'TFIDF', '5,15,17,4,21,13,3,7,18,8', 7, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (303, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'TFIDF', '8,19,16,21,6,9,10,12,4,11', 7, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (304, NULL, '希望靠近学校，适合学习，环境不要太吵', 'EMBEDDING', '4,15,5,17,11,12,3,8,9,16', 968, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (305, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'TFIDF', '10,20,8,21,19,2,14,5,3,17', 4, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (306, NULL, '想找面积大一点，生活配套好的房子', 'TFIDF', '16,9,6,10,8,19,21,15,11', 5, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (307, NULL, '想找三室，适合家庭居住，生活方便', 'TFIDF', '9,16,19,8,21,10,20,6,14,11', 6, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (308, NULL, '想找医院附近，有电梯，生活方便的两室', 'TFIDF', '19,21,8,13,10,2,14,20,6,11', 4, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (309, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'TFIDF', '20,10,8,3,14,4,19,2,11,17', 5, '2026-05-02 09:06:36');
INSERT INTO `recommend_record` VALUES (310, NULL, '想找安静小区，晚上休息不受影响', 'EMBEDDING', '15,17,5,8,11,10,20,4,12,18', 831, '2026-05-02 09:06:37');
INSERT INTO `recommend_record` VALUES (311, NULL, '考研期间居住，希望房子安静，同时交通方便', 'EMBEDDING', '5,15,17,11,12,4,20,8,3,9', 732, '2026-05-02 09:06:37');
INSERT INTO `recommend_record` VALUES (312, NULL, '想找安静的一室房源，适合学习和休息', 'EMBEDDING', '15,5,17,11,12,8,4,3,10,18', 789, '2026-05-02 09:06:38');
INSERT INTO `recommend_record` VALUES (313, NULL, '希望学习环境好，房子最好精装，周边安静', 'EMBEDDING', '5,3,4,15,12,17,8,9,11,21', 939, '2026-05-02 09:06:39');
INSERT INTO `recommend_record` VALUES (314, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'EMBEDDING', '8,20,9,12,17,10,16,3,4,11', 743, '2026-05-02 09:06:40');
INSERT INTO `recommend_record` VALUES (315, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'EMBEDDING', '20,10,8,9,19,14,16,21,3,12', 789, '2026-05-02 09:06:41');
INSERT INTO `recommend_record` VALUES (316, NULL, '想找面积大一点，生活配套好的房子', 'EMBEDDING', '8,3,4,20,9,16,21,10,17,12', 765, '2026-05-02 09:06:41');
INSERT INTO `recommend_record` VALUES (317, NULL, '想找三室，适合家庭居住，生活方便', 'EMBEDDING', '9,16,15,8,11,3,12,20,4,17', 828, '2026-05-02 09:06:42');
INSERT INTO `recommend_record` VALUES (318, NULL, '想找医院附近，有电梯，生活方便的两室', 'EMBEDDING', '19,20,14,10,8,12,13,6,4,2', 795, '2026-05-02 09:06:43');
INSERT INTO `recommend_record` VALUES (319, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'EMBEDDING', '20,10,9,8,12,4,11,16,3,17', 809, '2026-05-02 09:06:44');
INSERT INTO `recommend_record` VALUES (320, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'RULE', '12,11,5,4,3,18,15,6', 3, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (321, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'RULE', '18,11', 4, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (322, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'RULE', '12,11,5,4,3,9,18,17,15,13', 2, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (323, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'RULE', '11,5,12,4,3,15,18,6', 2, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (324, NULL, '学生租房，希望交通方便，价格不要太高', 'RULE', '12,11,4,3,20,19,18,14,9,7', 4, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (325, NULL, '预算不超过1200元，最好靠近学校或者大学', 'RULE', '11,4,18,15', 3, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (326, NULL, '想找交通方便，靠近公交站或者主路的房子', 'RULE', '20,19,18,14,12,11,7,6,4,3', 4, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (327, NULL, '希望房子靠近公交站，平时出行方便', 'RULE', '20,19,18,14,12,11,7,6,4,3', 3, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (328, NULL, '想找通勤方便，靠近主路或者车站的房子', 'RULE', '20,19,18,14,12,11,7,6,4,3', 3, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (329, NULL, '想找交通便利，同时房子最好是精装的公寓', 'RULE', '18,12,7,4,3,20,19,14,11,6', 3, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (330, NULL, '希望附近有公交站，周边生活方便', 'RULE', '20,19,6,18,14,12,11,7,4,3', 3, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (331, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'RULE', '20,19,6,18,14,12,11,7,4,3', 2, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (332, NULL, '想找精装修，可以直接入住的公寓', 'RULE', '21,18,13,12,10,7,4,3', 4, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (333, NULL, '希望房子装修好，能够拎包入住', 'RULE', '21,18,13,12,10,7,4,3', 4, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (334, NULL, '想找有电梯，精装，居住体验好一点的一室', 'RULE', '21,18,13,12,10,7,4,3', 2, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (335, NULL, '想找精装一室一厅，适合单人居住', 'RULE', '18,13,12,7,4,3,21,17,15,11', 3, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (336, NULL, '希望房子适合一个人住，装修不要太旧', 'RULE', '18,13,12,7,4,3,21,17,15,11', 2, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (337, NULL, '想找拎包入住的两室房源，装修新一点', 'RULE', '21,18,13,12,10,7,4,3', 2, '2026-05-02 09:07:09');
INSERT INTO `recommend_record` VALUES (338, NULL, '想找安静一点，适合考研复习的房子', 'RULE', '11,5,12,9,4,3,17,15', 2, '2026-05-02 09:07:10');
INSERT INTO `recommend_record` VALUES (339, NULL, '希望靠近学校，适合学习，环境不要太吵', 'RULE', '11,5,12,9,4,3,17,15', 3, '2026-05-02 09:07:10');
INSERT INTO `recommend_record` VALUES (340, NULL, '想找安静小区，晚上休息不受影响', 'RULE', '17,15,11,5', 2, '2026-05-02 09:07:10');
INSERT INTO `recommend_record` VALUES (341, NULL, '考研期间居住，希望房子安静，同时交通方便', 'RULE', '11,12,4,3,5,20,19,18,14,9', 2, '2026-05-02 09:07:10');
INSERT INTO `recommend_record` VALUES (342, NULL, '想找安静的一室房源，适合学习和休息', 'RULE', '17,15,11,5', 2, '2026-05-02 09:07:10');
INSERT INTO `recommend_record` VALUES (343, NULL, '希望学习环境好，房子最好精装，周边安静', 'RULE', '21,18,13,12,10,7,4,3,17,15', 3, '2026-05-02 09:07:10');
INSERT INTO `recommend_record` VALUES (344, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'RULE', '21,20,19,16,10,9,8,6', 1, '2026-05-02 09:07:10');
INSERT INTO `recommend_record` VALUES (345, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'RULE', '21,20,19,16,10,9,8,6', 3, '2026-05-02 09:07:10');
INSERT INTO `recommend_record` VALUES (346, NULL, '想找面积大一点，生活配套好的房子', 'RULE', '21,20,19,16,10,9,8,6', 2, '2026-05-02 09:07:10');
INSERT INTO `recommend_record` VALUES (347, NULL, '想找三室，适合家庭居住，生活方便', 'RULE', '21,20,19,16,10,9,8,6', 2, '2026-05-02 09:07:10');
INSERT INTO `recommend_record` VALUES (348, NULL, '想找医院附近，有电梯，生活方便的两室', 'RULE', '21,20,19,16,10,9,8,6', 3, '2026-05-02 09:07:10');
INSERT INTO `recommend_record` VALUES (349, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'RULE', '21,20,19,16,10,9,8,6', 2, '2026-05-02 09:07:10');
INSERT INTO `recommend_record` VALUES (350, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'EMBEDDING', '3,4,11,5,12,18,15,6', 395, '2026-05-02 09:07:14');
INSERT INTO `recommend_record` VALUES (351, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'EMBEDDING', '11,18', 164, '2026-05-02 09:07:14');
INSERT INTO `recommend_record` VALUES (352, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'EMBEDDING', '12,4,11,5,3,15,17,18,8,9', 856, '2026-05-02 09:07:15');
INSERT INTO `recommend_record` VALUES (353, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'EMBEDDING', '5,15,11,4,3,18,12,6', 392, '2026-05-02 09:07:16');
INSERT INTO `recommend_record` VALUES (354, NULL, '学生租房，希望交通方便，价格不要太高', 'EMBEDDING', '11,4,12,18,3,7,20,5,9,15', 844, '2026-05-02 09:07:16');
INSERT INTO `recommend_record` VALUES (355, NULL, '预算不超过1200元，最好靠近学校或者大学', 'EMBEDDING', '4,11,18,15', 234, '2026-05-02 09:07:17');
INSERT INTO `recommend_record` VALUES (356, NULL, '想找交通方便，靠近公交站或者主路的房子', 'EMBEDDING', '7,12,4,6,20,14,11,10,18,2', 807, '2026-05-02 09:07:18');
INSERT INTO `recommend_record` VALUES (357, NULL, '希望房子靠近公交站，平时出行方便', 'EMBEDDING', '7,20,6,14,2,10,4,12,11,17', 797, '2026-05-02 09:07:18');
INSERT INTO `recommend_record` VALUES (358, NULL, '想找通勤方便，靠近主路或者车站的房子', 'EMBEDDING', '7,4,12,20,11,14,6,10,18,9', 790, '2026-05-02 09:07:19');
INSERT INTO `recommend_record` VALUES (359, NULL, '想找交通便利，同时房子最好是精装的公寓', 'EMBEDDING', '3,7,4,8,21,12,20,16,10,18', 785, '2026-05-02 09:07:20');
INSERT INTO `recommend_record` VALUES (360, NULL, '希望附近有公交站，周边生活方便', 'EMBEDDING', '6,7,14,20,2,10,17,19,4,11', 832, '2026-05-02 09:07:21');
INSERT INTO `recommend_record` VALUES (361, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'EMBEDDING', '20,10,9,4,14,6,12,7,11,3', 800, '2026-05-02 09:07:22');
INSERT INTO `recommend_record` VALUES (362, NULL, '想找精装修，可以直接入住的公寓', 'EMBEDDING', '3,21,4,7,16,18,8,13,12,17', 802, '2026-05-02 09:07:22');
INSERT INTO `recommend_record` VALUES (363, NULL, '希望房子装修好，能够拎包入住', 'EMBEDDING', '21,3,4,13,8,7,16,9,12,17', 840, '2026-05-02 09:07:23');
INSERT INTO `recommend_record` VALUES (364, NULL, '想找有电梯，精装，居住体验好一点的一室', 'EMBEDDING', '13,3,8,16,21,4,19,7,20,17', 837, '2026-05-02 09:07:24');
INSERT INTO `recommend_record` VALUES (365, NULL, '想找精装一室一厅，适合单人居住', 'EMBEDDING', '8,3,4,16,21,13,17,18,9,20', 787, '2026-05-02 09:07:25');
INSERT INTO `recommend_record` VALUES (366, NULL, '希望房子适合一个人住，装修不要太旧', 'EMBEDDING', '8,21,3,4,17,16,13,15,12,18', 795, '2026-05-02 09:07:26');
INSERT INTO `recommend_record` VALUES (367, NULL, '想找拎包入住的两室房源，装修新一点', 'EMBEDDING', '21,8,3,4,13,20,10,16,19,12', 801, '2026-05-02 09:07:27');
INSERT INTO `recommend_record` VALUES (368, NULL, '想找安静一点，适合考研复习的房子', 'EMBEDDING', '5,15,17,12,4,8,11,9,20,16', 808, '2026-05-02 09:07:27');
INSERT INTO `recommend_record` VALUES (369, NULL, '希望靠近学校，适合学习，环境不要太吵', 'EMBEDDING', '4,15,5,17,11,12,3,8,9,16', 795, '2026-05-02 09:07:28');
INSERT INTO `recommend_record` VALUES (370, NULL, '想找安静小区，晚上休息不受影响', 'EMBEDDING', '15,17,5,8,11,10,20,4,12,18', 847, '2026-05-02 09:07:29');
INSERT INTO `recommend_record` VALUES (371, NULL, '考研期间居住，希望房子安静，同时交通方便', 'EMBEDDING', '5,15,17,11,12,4,20,8,3,9', 857, '2026-05-02 09:07:30');
INSERT INTO `recommend_record` VALUES (372, NULL, '想找安静的一室房源，适合学习和休息', 'EMBEDDING', '15,5,17,11,12,8,4,3,10,18', 941, '2026-05-02 09:07:31');
INSERT INTO `recommend_record` VALUES (373, NULL, '希望学习环境好，房子最好精装，周边安静', 'EMBEDDING', '5,3,4,15,12,17,8,9,11,21', 805, '2026-05-02 09:07:32');
INSERT INTO `recommend_record` VALUES (374, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'EMBEDDING', '8,20,9,12,17,10,16,3,4,11', 796, '2026-05-02 09:07:32');
INSERT INTO `recommend_record` VALUES (375, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'EMBEDDING', '20,10,8,9,19,14,16,21,3,12', 742, '2026-05-02 09:07:33');
INSERT INTO `recommend_record` VALUES (376, NULL, '想找面积大一点，生活配套好的房子', 'EMBEDDING', '8,3,4,20,9,16,21,10,17,12', 773, '2026-05-02 09:07:34');
INSERT INTO `recommend_record` VALUES (377, NULL, '想找三室，适合家庭居住，生活方便', 'EMBEDDING', '9,16,15,8,11,3,12,20,4,17', 761, '2026-05-02 09:07:35');
INSERT INTO `recommend_record` VALUES (378, NULL, '想找医院附近，有电梯，生活方便的两室', 'EMBEDDING', '19,20,14,10,8,12,13,6,4,2', 794, '2026-05-02 09:07:36');
INSERT INTO `recommend_record` VALUES (379, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'EMBEDDING', '20,10,9,8,12,4,11,16,3,17', 736, '2026-05-02 09:07:36');
INSERT INTO `recommend_record` VALUES (380, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'RULE', '12,11,5,4,3', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (381, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'RULE', '18,11', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (382, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'RULE', '12,11,5,4,3', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (383, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'RULE', '11,5,12,4,3', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (384, NULL, '学生租房，希望交通方便，价格不要太高', 'RULE', '12,11,4,3,20', 1, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (385, NULL, '预算不超过1200元，最好靠近学校或者大学', 'RULE', '11,4,18,15', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (386, NULL, '想找交通方便，靠近公交站或者主路的房子', 'RULE', '20,19,18,14,12', 4, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (387, NULL, '希望房子靠近公交站，平时出行方便', 'RULE', '20,19,18,14,12', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (388, NULL, '想找通勤方便，靠近主路或者车站的房子', 'RULE', '20,19,18,14,12', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (389, NULL, '想找交通便利，同时房子最好是精装的公寓', 'RULE', '18,12,7,4,3', 1, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (390, NULL, '希望附近有公交站，周边生活方便', 'RULE', '20,19,6,18,14', 3, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (391, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'RULE', '20,19,6,18,14', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (392, NULL, '想找精装修，可以直接入住的公寓', 'RULE', '21,18,13,12,10', 1, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (393, NULL, '希望房子装修好，能够拎包入住', 'RULE', '21,18,13,12,10', 1, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (394, NULL, '想找有电梯，精装，居住体验好一点的一室', 'RULE', '21,18,13,12,10', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (395, NULL, '想找精装一室一厅，适合单人居住', 'RULE', '18,13,12,7,4', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (396, NULL, '希望房子适合一个人住，装修不要太旧', 'RULE', '18,13,12,7,4', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (397, NULL, '想找拎包入住的两室房源，装修新一点', 'RULE', '21,18,13,12,10', 3, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (398, NULL, '想找安静一点，适合考研复习的房子', 'RULE', '11,5,12,9,4', 1, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (399, NULL, '希望靠近学校，适合学习，环境不要太吵', 'RULE', '11,5,12,9,4', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (400, NULL, '想找安静小区，晚上休息不受影响', 'RULE', '17,15,11,5', 1, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (401, NULL, '考研期间居住，希望房子安静，同时交通方便', 'RULE', '11,12,4,3,5', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (402, NULL, '想找安静的一室房源，适合学习和休息', 'RULE', '17,15,11,5', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (403, NULL, '希望学习环境好，房子最好精装，周边安静', 'RULE', '21,18,13,12,10', 3, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (404, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'RULE', '21,20,19,16,10', 3, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (405, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'RULE', '21,20,19,16,10', 1, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (406, NULL, '想找面积大一点，生活配套好的房子', 'RULE', '21,20,19,16,10', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (407, NULL, '想找三室，适合家庭居住，生活方便', 'RULE', '21,20,19,16,10', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (408, NULL, '想找医院附近，有电梯，生活方便的两室', 'RULE', '21,20,19,16,10', 2, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (409, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'RULE', '21,20,19,16,10', 1, '2026-05-02 09:13:15');
INSERT INTO `recommend_record` VALUES (410, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'EMBEDDING', '3,4,11,5,12', 399, '2026-05-02 09:13:19');
INSERT INTO `recommend_record` VALUES (411, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'EMBEDDING', '11,18', 168, '2026-05-02 09:13:20');
INSERT INTO `recommend_record` VALUES (412, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'EMBEDDING', '12,4,11,5,3', 1046, '2026-05-02 09:13:21');
INSERT INTO `recommend_record` VALUES (413, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'EMBEDDING', '5,15,11,4,3', 436, '2026-05-02 09:13:21');
INSERT INTO `recommend_record` VALUES (414, NULL, '学生租房，希望交通方便，价格不要太高', 'EMBEDDING', '11,4,12,18,3', 839, '2026-05-02 09:13:22');
INSERT INTO `recommend_record` VALUES (415, NULL, '预算不超过1200元，最好靠近学校或者大学', 'EMBEDDING', '4,11,18,15', 269, '2026-05-02 09:13:22');
INSERT INTO `recommend_record` VALUES (416, NULL, '想找交通方便，靠近公交站或者主路的房子', 'EMBEDDING', '7,12,4,6,20', 829, '2026-05-02 09:13:23');
INSERT INTO `recommend_record` VALUES (417, NULL, '希望房子靠近公交站，平时出行方便', 'EMBEDDING', '7,20,6,14,2', 787, '2026-05-02 09:13:24');
INSERT INTO `recommend_record` VALUES (418, NULL, '想找通勤方便，靠近主路或者车站的房子', 'EMBEDDING', '7,4,12,20,11', 809, '2026-05-02 09:13:25');
INSERT INTO `recommend_record` VALUES (419, NULL, '想找交通便利，同时房子最好是精装的公寓', 'EMBEDDING', '3,7,4,8,21', 818, '2026-05-02 09:13:26');
INSERT INTO `recommend_record` VALUES (420, NULL, '希望附近有公交站，周边生活方便', 'EMBEDDING', '6,7,14,20,2', 791, '2026-05-02 09:13:26');
INSERT INTO `recommend_record` VALUES (421, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'EMBEDDING', '20,10,9,4,14', 830, '2026-05-02 09:13:27');
INSERT INTO `recommend_record` VALUES (422, NULL, '想找精装修，可以直接入住的公寓', 'EMBEDDING', '3,21,4,7,16', 794, '2026-05-02 09:13:28');
INSERT INTO `recommend_record` VALUES (423, NULL, '希望房子装修好，能够拎包入住', 'EMBEDDING', '21,3,4,13,8', 815, '2026-05-02 09:13:29');
INSERT INTO `recommend_record` VALUES (424, NULL, '想找有电梯，精装，居住体验好一点的一室', 'EMBEDDING', '13,3,8,16,21', 834, '2026-05-02 09:13:30');
INSERT INTO `recommend_record` VALUES (425, NULL, '想找精装一室一厅，适合单人居住', 'EMBEDDING', '8,3,4,16,21', 869, '2026-05-02 09:13:31');
INSERT INTO `recommend_record` VALUES (426, NULL, '希望房子适合一个人住，装修不要太旧', 'EMBEDDING', '8,21,3,4,17', 913, '2026-05-02 09:13:31');
INSERT INTO `recommend_record` VALUES (427, NULL, '想找拎包入住的两室房源，装修新一点', 'EMBEDDING', '21,8,3,4,13', 815, '2026-05-02 09:13:32');
INSERT INTO `recommend_record` VALUES (428, NULL, '想找安静一点，适合考研复习的房子', 'EMBEDDING', '5,15,17,12,4', 825, '2026-05-02 09:13:33');
INSERT INTO `recommend_record` VALUES (429, NULL, '希望靠近学校，适合学习，环境不要太吵', 'EMBEDDING', '4,15,5,17,11', 781, '2026-05-02 09:13:34');
INSERT INTO `recommend_record` VALUES (430, NULL, '想找安静小区，晚上休息不受影响', 'EMBEDDING', '15,17,5,8,11', 774, '2026-05-02 09:13:35');
INSERT INTO `recommend_record` VALUES (431, NULL, '考研期间居住，希望房子安静，同时交通方便', 'EMBEDDING', '5,15,17,11,12', 784, '2026-05-02 09:13:36');
INSERT INTO `recommend_record` VALUES (432, NULL, '想找安静的一室房源，适合学习和休息', 'EMBEDDING', '15,5,17,11,12', 784, '2026-05-02 09:13:36');
INSERT INTO `recommend_record` VALUES (433, NULL, '希望学习环境好，房子最好精装，周边安静', 'EMBEDDING', '5,3,4,15,12', 752, '2026-05-02 09:13:37');
INSERT INTO `recommend_record` VALUES (434, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'EMBEDDING', '8,20,9,12,17', 755, '2026-05-02 09:13:38');
INSERT INTO `recommend_record` VALUES (435, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'EMBEDDING', '20,10,8,9,19', 729, '2026-05-02 09:13:39');
INSERT INTO `recommend_record` VALUES (436, NULL, '想找面积大一点，生活配套好的房子', 'EMBEDDING', '8,3,4,20,9', 738, '2026-05-02 09:13:39');
INSERT INTO `recommend_record` VALUES (437, NULL, '想找三室，适合家庭居住，生活方便', 'EMBEDDING', '9,16,15,8,11', 736, '2026-05-02 09:13:40');
INSERT INTO `recommend_record` VALUES (438, NULL, '想找医院附近，有电梯，生活方便的两室', 'EMBEDDING', '19,20,14,10,8', 749, '2026-05-02 09:13:41');
INSERT INTO `recommend_record` VALUES (439, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'EMBEDDING', '20,10,9,8,12', 729, '2026-05-02 09:13:42');
INSERT INTO `recommend_record` VALUES (440, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'RULE', '12,11,5,4,3', 2, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (441, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'RULE', '18,11', 1, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (442, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'RULE', '12,11,5,4,3', 2, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (443, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'RULE', '11,5,12,4,3', 1, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (444, NULL, '学生租房，希望交通方便，价格不要太高', 'RULE', '12,11,4,3,20', 3, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (445, NULL, '预算不超过1200元，最好靠近学校或者大学', 'RULE', '11,4,18,15', 2, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (446, NULL, '想找交通方便，靠近公交站或者主路的房子', 'RULE', '20,19,18,14,12', 2, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (447, NULL, '希望房子靠近公交站，平时出行方便', 'RULE', '20,19,18,14,12', 1, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (448, NULL, '想找通勤方便，靠近主路或者车站的房子', 'RULE', '20,19,18,14,12', 3, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (449, NULL, '想找交通便利，同时房子最好是精装的公寓', 'RULE', '18,12,7,4,3', 3, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (450, NULL, '希望附近有公交站，周边生活方便', 'RULE', '20,19,6,18,14', 2, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (451, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'RULE', '20,19,6,18,14', 1, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (452, NULL, '想找精装修，可以直接入住的公寓', 'RULE', '21,18,13,12,10', 1, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (453, NULL, '希望房子装修好，能够拎包入住', 'RULE', '21,18,13,12,10', 1, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (454, NULL, '想找有电梯，精装，居住体验好一点的一室', 'RULE', '21,18,13,12,10', 2, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (455, NULL, '想找精装一室一厅，适合单人居住', 'RULE', '18,13,12,7,4', 4, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (456, NULL, '希望房子适合一个人住，装修不要太旧', 'RULE', '18,13,12,7,4', 2, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (457, NULL, '想找拎包入住的两室房源，装修新一点', 'RULE', '21,18,13,12,10', 1, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (458, NULL, '想找安静一点，适合考研复习的房子', 'RULE', '11,5,12,9,4', 2, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (459, NULL, '希望靠近学校，适合学习，环境不要太吵', 'RULE', '11,5,12,9,4', 1, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (460, NULL, '想找安静小区，晚上休息不受影响', 'RULE', '17,15,11,5', 3, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (461, NULL, '考研期间居住，希望房子安静，同时交通方便', 'RULE', '11,12,4,3,5', 4, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (462, NULL, '想找安静的一室房源，适合学习和休息', 'RULE', '17,15,11,5', 2, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (463, NULL, '希望学习环境好，房子最好精装，周边安静', 'RULE', '21,18,13,12,10', 1, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (464, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'RULE', '21,20,19,16,10', 5, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (465, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'RULE', '21,20,19,16,10', 8, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (466, NULL, '想找面积大一点，生活配套好的房子', 'RULE', '21,20,19,16,10', 3, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (467, NULL, '想找三室，适合家庭居住，生活方便', 'RULE', '21,20,19,16,10', 4, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (468, NULL, '想找医院附近，有电梯，生活方便的两室', 'RULE', '21,20,19,16,10', 4, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (469, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'RULE', '21,20,19,16,10', 2, '2026-05-02 09:14:18');
INSERT INTO `recommend_record` VALUES (470, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'EMBEDDING', '3,4,11', 401, '2026-05-02 09:14:22');
INSERT INTO `recommend_record` VALUES (471, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'EMBEDDING', '11,18', 173, '2026-05-02 09:14:22');
INSERT INTO `recommend_record` VALUES (472, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'EMBEDDING', '12,4,11', 908, '2026-05-02 09:14:23');
INSERT INTO `recommend_record` VALUES (473, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'EMBEDDING', '5,15,11', 397, '2026-05-02 09:14:24');
INSERT INTO `recommend_record` VALUES (474, NULL, '学生租房，希望交通方便，价格不要太高', 'EMBEDDING', '11,4,12', 875, '2026-05-02 09:14:25');
INSERT INTO `recommend_record` VALUES (475, NULL, '预算不超过1200元，最好靠近学校或者大学', 'EMBEDDING', '4,11,18', 240, '2026-05-02 09:14:25');
INSERT INTO `recommend_record` VALUES (476, NULL, '想找交通方便，靠近公交站或者主路的房子', 'EMBEDDING', '7,12,4', 853, '2026-05-02 09:14:26');
INSERT INTO `recommend_record` VALUES (477, NULL, '希望房子靠近公交站，平时出行方便', 'EMBEDDING', '7,20,6', 848, '2026-05-02 09:14:27');
INSERT INTO `recommend_record` VALUES (478, NULL, '想找通勤方便，靠近主路或者车站的房子', 'EMBEDDING', '7,4,12', 831, '2026-05-02 09:14:28');
INSERT INTO `recommend_record` VALUES (479, NULL, '想找交通便利，同时房子最好是精装的公寓', 'EMBEDDING', '3,7,4', 827, '2026-05-02 09:14:28');
INSERT INTO `recommend_record` VALUES (480, NULL, '希望附近有公交站，周边生活方便', 'EMBEDDING', '6,7,14', 819, '2026-05-02 09:14:29');
INSERT INTO `recommend_record` VALUES (481, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'EMBEDDING', '20,10,9', 820, '2026-05-02 09:14:30');
INSERT INTO `recommend_record` VALUES (482, NULL, '想找精装修，可以直接入住的公寓', 'EMBEDDING', '3,21,4', 853, '2026-05-02 09:14:31');
INSERT INTO `recommend_record` VALUES (483, NULL, '希望房子装修好，能够拎包入住', 'EMBEDDING', '21,3,4', 830, '2026-05-02 09:14:32');
INSERT INTO `recommend_record` VALUES (484, NULL, '想找有电梯，精装，居住体验好一点的一室', 'EMBEDDING', '13,3,8', 842, '2026-05-02 09:14:33');
INSERT INTO `recommend_record` VALUES (485, NULL, '想找精装一室一厅，适合单人居住', 'EMBEDDING', '8,3,4', 878, '2026-05-02 09:14:33');
INSERT INTO `recommend_record` VALUES (486, NULL, '希望房子适合一个人住，装修不要太旧', 'EMBEDDING', '8,21,3', 849, '2026-05-02 09:14:34');
INSERT INTO `recommend_record` VALUES (487, NULL, '想找拎包入住的两室房源，装修新一点', 'EMBEDDING', '21,8,3', 843, '2026-05-02 09:14:35');
INSERT INTO `recommend_record` VALUES (488, NULL, '想找安静一点，适合考研复习的房子', 'EMBEDDING', '5,15,17', 853, '2026-05-02 09:14:36');
INSERT INTO `recommend_record` VALUES (489, NULL, '希望靠近学校，适合学习，环境不要太吵', 'EMBEDDING', '4,15,5', 840, '2026-05-02 09:14:37');
INSERT INTO `recommend_record` VALUES (490, NULL, '想找安静小区，晚上休息不受影响', 'EMBEDDING', '15,17,5', 829, '2026-05-02 09:14:38');
INSERT INTO `recommend_record` VALUES (491, NULL, '考研期间居住，希望房子安静，同时交通方便', 'EMBEDDING', '5,15,17', 838, '2026-05-02 09:14:39');
INSERT INTO `recommend_record` VALUES (492, NULL, '想找安静的一室房源，适合学习和休息', 'EMBEDDING', '15,5,17', 850, '2026-05-02 09:14:39');
INSERT INTO `recommend_record` VALUES (493, NULL, '希望学习环境好，房子最好精装，周边安静', 'EMBEDDING', '5,3,4', 830, '2026-05-02 09:14:40');
INSERT INTO `recommend_record` VALUES (494, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'EMBEDDING', '8,20,9', 864, '2026-05-02 09:14:41');
INSERT INTO `recommend_record` VALUES (495, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'EMBEDDING', '20,10,8', 845, '2026-05-02 09:14:42');
INSERT INTO `recommend_record` VALUES (496, NULL, '想找面积大一点，生活配套好的房子', 'EMBEDDING', '8,3,4', 759, '2026-05-02 09:14:43');
INSERT INTO `recommend_record` VALUES (497, NULL, '想找三室，适合家庭居住，生活方便', 'EMBEDDING', '9,16,15', 765, '2026-05-02 09:14:44');
INSERT INTO `recommend_record` VALUES (498, NULL, '想找医院附近，有电梯，生活方便的两室', 'EMBEDDING', '19,20,14', 784, '2026-05-02 09:14:44');
INSERT INTO `recommend_record` VALUES (499, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'EMBEDDING', '20,10,9', 784, '2026-05-02 09:14:45');
INSERT INTO `recommend_record` VALUES (500, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'RULE', '12,11,5,4,3', 2, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (501, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'RULE', '18,11', 1, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (502, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'RULE', '12,11,5,4,3', 1, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (503, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'RULE', '11,5,12,4,3', 1, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (504, NULL, '学生租房，希望交通方便，价格不要太高', 'RULE', '12,11,4,3,20', 4, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (505, NULL, '预算不超过1200元，最好靠近学校或者大学', 'RULE', '11,4,18,15', 2, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (506, NULL, '想找交通方便，靠近公交站或者主路的房子', 'RULE', '20,19,18,14,12', 3, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (507, NULL, '希望房子靠近公交站，平时出行方便', 'RULE', '20,19,18,14,12', 2, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (508, NULL, '想找通勤方便，靠近主路或者车站的房子', 'RULE', '20,19,18,14,12', 2, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (509, NULL, '想找交通便利，同时房子最好是精装的公寓', 'RULE', '18,12,7,4,3', 2, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (510, NULL, '希望附近有公交站，周边生活方便', 'RULE', '20,19,6,18,14', 4, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (511, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'RULE', '20,19,6,18,14', 2, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (512, NULL, '想找精装修，可以直接入住的公寓', 'RULE', '21,18,13,12,10', 2, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (513, NULL, '希望房子装修好，能够拎包入住', 'RULE', '21,18,13,12,10', 2, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (514, NULL, '想找有电梯，精装，居住体验好一点的一室', 'RULE', '21,18,13,12,10', 3, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (515, NULL, '想找精装一室一厅，适合单人居住', 'RULE', '18,13,12,7,4', 1, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (516, NULL, '希望房子适合一个人住，装修不要太旧', 'RULE', '18,13,12,7,4', 0, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (517, NULL, '想找拎包入住的两室房源，装修新一点', 'RULE', '21,18,13,12,10', 4, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (518, NULL, '想找安静一点，适合考研复习的房子', 'RULE', '11,5,12,9,4', 4, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (519, NULL, '希望靠近学校，适合学习，环境不要太吵', 'RULE', '11,5,12,9,4', 5, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (520, NULL, '想找安静小区，晚上休息不受影响', 'RULE', '17,15,11,5', 3, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (521, NULL, '考研期间居住，希望房子安静，同时交通方便', 'RULE', '11,12,4,3,5', 4, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (522, NULL, '想找安静的一室房源，适合学习和休息', 'RULE', '17,15,11,5', 3, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (523, NULL, '希望学习环境好，房子最好精装，周边安静', 'RULE', '21,18,13,12,10', 2, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (524, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'RULE', '21,20,19,16,10', 2, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (525, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'RULE', '21,20,19,16,10', 1, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (526, NULL, '想找面积大一点，生活配套好的房子', 'RULE', '21,20,19,16,10', 2, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (527, NULL, '想找三室，适合家庭居住，生活方便', 'RULE', '21,20,19,16,10', 4, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (528, NULL, '想找医院附近，有电梯，生活方便的两室', 'RULE', '21,20,19,16,10', 3, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (529, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'RULE', '21,20,19,16,10', 5, '2026-05-02 09:18:05');
INSERT INTO `recommend_record` VALUES (530, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'EMBEDDING', '3,4,11', 367, '2026-05-02 09:18:09');
INSERT INTO `recommend_record` VALUES (531, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'EMBEDDING', '11,18', 157, '2026-05-02 09:18:09');
INSERT INTO `recommend_record` VALUES (532, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'EMBEDDING', '12,4,11', 791, '2026-05-02 09:18:10');
INSERT INTO `recommend_record` VALUES (533, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'EMBEDDING', '5,15,11', 392, '2026-05-02 09:18:10');
INSERT INTO `recommend_record` VALUES (534, NULL, '学生租房，希望交通方便，价格不要太高', 'EMBEDDING', '11,4,12', 795, '2026-05-02 09:18:11');
INSERT INTO `recommend_record` VALUES (535, NULL, '预算不超过1200元，最好靠近学校或者大学', 'EMBEDDING', '4,11,18', 230, '2026-05-02 09:18:11');
INSERT INTO `recommend_record` VALUES (536, NULL, '想找交通方便，靠近公交站或者主路的房子', 'EMBEDDING', '7,12,4', 793, '2026-05-02 09:18:12');
INSERT INTO `recommend_record` VALUES (537, NULL, '希望房子靠近公交站，平时出行方便', 'EMBEDDING', '7,20,6', 808, '2026-05-02 09:18:13');
INSERT INTO `recommend_record` VALUES (538, NULL, '想找通勤方便，靠近主路或者车站的房子', 'EMBEDDING', '7,4,12', 809, '2026-05-02 09:18:14');
INSERT INTO `recommend_record` VALUES (539, NULL, '想找交通便利，同时房子最好是精装的公寓', 'EMBEDDING', '3,7,4', 805, '2026-05-02 09:18:15');
INSERT INTO `recommend_record` VALUES (540, NULL, '希望附近有公交站，周边生活方便', 'EMBEDDING', '6,7,14', 790, '2026-05-02 09:18:16');
INSERT INTO `recommend_record` VALUES (541, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'EMBEDDING', '20,10,9', 829, '2026-05-02 09:18:16');
INSERT INTO `recommend_record` VALUES (542, NULL, '想找精装修，可以直接入住的公寓', 'EMBEDDING', '3,21,4', 810, '2026-05-02 09:18:17');
INSERT INTO `recommend_record` VALUES (543, NULL, '希望房子装修好，能够拎包入住', 'EMBEDDING', '21,3,4', 815, '2026-05-02 09:18:18');
INSERT INTO `recommend_record` VALUES (544, NULL, '想找有电梯，精装，居住体验好一点的一室', 'EMBEDDING', '13,3,8', 820, '2026-05-02 09:18:19');
INSERT INTO `recommend_record` VALUES (545, NULL, '想找精装一室一厅，适合单人居住', 'EMBEDDING', '8,3,4', 817, '2026-05-02 09:18:20');
INSERT INTO `recommend_record` VALUES (546, NULL, '希望房子适合一个人住，装修不要太旧', 'EMBEDDING', '8,21,3', 820, '2026-05-02 09:18:21');
INSERT INTO `recommend_record` VALUES (547, NULL, '想找拎包入住的两室房源，装修新一点', 'EMBEDDING', '21,8,3', 828, '2026-05-02 09:18:21');
INSERT INTO `recommend_record` VALUES (548, NULL, '想找安静一点，适合考研复习的房子', 'EMBEDDING', '5,15,17', 804, '2026-05-02 09:18:22');
INSERT INTO `recommend_record` VALUES (549, NULL, '希望靠近学校，适合学习，环境不要太吵', 'EMBEDDING', '4,15,5', 814, '2026-05-02 09:18:23');
INSERT INTO `recommend_record` VALUES (550, NULL, '想找安静小区，晚上休息不受影响', 'EMBEDDING', '15,17,5', 804, '2026-05-02 09:18:24');
INSERT INTO `recommend_record` VALUES (551, NULL, '考研期间居住，希望房子安静，同时交通方便', 'EMBEDDING', '5,15,17', 828, '2026-05-02 09:18:25');
INSERT INTO `recommend_record` VALUES (552, NULL, '想找安静的一室房源，适合学习和休息', 'EMBEDDING', '15,5,17', 797, '2026-05-02 09:18:25');
INSERT INTO `recommend_record` VALUES (553, NULL, '希望学习环境好，房子最好精装，周边安静', 'EMBEDDING', '5,3,4', 797, '2026-05-02 09:18:26');
INSERT INTO `recommend_record` VALUES (554, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'EMBEDDING', '8,20,9', 799, '2026-05-02 09:18:27');
INSERT INTO `recommend_record` VALUES (555, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'EMBEDDING', '20,10,8', 792, '2026-05-02 09:18:28');
INSERT INTO `recommend_record` VALUES (556, NULL, '想找面积大一点，生活配套好的房子', 'EMBEDDING', '8,3,4', 816, '2026-05-02 09:18:29');
INSERT INTO `recommend_record` VALUES (557, NULL, '想找三室，适合家庭居住，生活方便', 'EMBEDDING', '9,16,15', 823, '2026-05-02 09:18:30');
INSERT INTO `recommend_record` VALUES (558, NULL, '想找医院附近，有电梯，生活方便的两室', 'EMBEDDING', '19,20,14', 827, '2026-05-02 09:18:30');
INSERT INTO `recommend_record` VALUES (559, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'EMBEDDING', '20,10,9', 855, '2026-05-02 09:18:31');
INSERT INTO `recommend_record` VALUES (560, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'EMBEDDING', '3,4,11', 359, '2026-05-02 09:18:40');
INSERT INTO `recommend_record` VALUES (561, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'EMBEDDING', '11,18', 151, '2026-05-02 09:18:40');
INSERT INTO `recommend_record` VALUES (562, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'EMBEDDING', '12,4,11', 789, '2026-05-02 09:18:41');
INSERT INTO `recommend_record` VALUES (563, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'EMBEDDING', '5,15,11', 373, '2026-05-02 09:18:41');
INSERT INTO `recommend_record` VALUES (564, NULL, '学生租房，希望交通方便，价格不要太高', 'EMBEDDING', '11,4,12', 803, '2026-05-02 09:18:42');
INSERT INTO `recommend_record` VALUES (565, NULL, '预算不超过1200元，最好靠近学校或者大学', 'EMBEDDING', '4,11,18', 234, '2026-05-02 09:18:42');
INSERT INTO `recommend_record` VALUES (566, NULL, '想找交通方便，靠近公交站或者主路的房子', 'EMBEDDING', '7,12,4', 804, '2026-05-02 09:18:43');
INSERT INTO `recommend_record` VALUES (567, NULL, '希望房子靠近公交站，平时出行方便', 'EMBEDDING', '7,20,6', 810, '2026-05-02 09:18:44');
INSERT INTO `recommend_record` VALUES (568, NULL, '想找通勤方便，靠近主路或者车站的房子', 'EMBEDDING', '7,4,12', 803, '2026-05-02 09:18:45');
INSERT INTO `recommend_record` VALUES (569, NULL, '想找交通便利，同时房子最好是精装的公寓', 'EMBEDDING', '3,7,4', 808, '2026-05-02 09:18:46');
INSERT INTO `recommend_record` VALUES (570, NULL, '希望附近有公交站，周边生活方便', 'EMBEDDING', '6,7,14', 800, '2026-05-02 09:18:46');
INSERT INTO `recommend_record` VALUES (571, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'EMBEDDING', '20,10,9', 796, '2026-05-02 09:18:47');
INSERT INTO `recommend_record` VALUES (572, NULL, '想找精装修，可以直接入住的公寓', 'EMBEDDING', '3,21,4', 777, '2026-05-02 09:18:48');
INSERT INTO `recommend_record` VALUES (573, NULL, '希望房子装修好，能够拎包入住', 'EMBEDDING', '21,3,4', 762, '2026-05-02 09:18:49');
INSERT INTO `recommend_record` VALUES (574, NULL, '想找有电梯，精装，居住体验好一点的一室', 'EMBEDDING', '13,3,8', 784, '2026-05-02 09:18:50');
INSERT INTO `recommend_record` VALUES (575, NULL, '想找精装一室一厅，适合单人居住', 'EMBEDDING', '8,3,4', 781, '2026-05-02 09:18:50');
INSERT INTO `recommend_record` VALUES (576, NULL, '希望房子适合一个人住，装修不要太旧', 'EMBEDDING', '8,21,3', 811, '2026-05-02 09:18:51');
INSERT INTO `recommend_record` VALUES (577, NULL, '想找拎包入住的两室房源，装修新一点', 'EMBEDDING', '21,8,3', 811, '2026-05-02 09:18:52');
INSERT INTO `recommend_record` VALUES (578, NULL, '想找安静一点，适合考研复习的房子', 'EMBEDDING', '5,15,17', 806, '2026-05-02 09:18:53');
INSERT INTO `recommend_record` VALUES (579, NULL, '希望靠近学校，适合学习，环境不要太吵', 'EMBEDDING', '4,15,5', 829, '2026-05-02 09:18:54');
INSERT INTO `recommend_record` VALUES (580, NULL, '想找安静小区，晚上休息不受影响', 'EMBEDDING', '15,17,5', 819, '2026-05-02 09:18:55');
INSERT INTO `recommend_record` VALUES (581, NULL, '考研期间居住，希望房子安静，同时交通方便', 'EMBEDDING', '5,15,17', 811, '2026-05-02 09:18:55');
INSERT INTO `recommend_record` VALUES (582, NULL, '想找安静的一室房源，适合学习和休息', 'EMBEDDING', '15,5,17', 803, '2026-05-02 09:18:56');
INSERT INTO `recommend_record` VALUES (583, NULL, '希望学习环境好，房子最好精装，周边安静', 'EMBEDDING', '5,3,4', 825, '2026-05-02 09:18:57');
INSERT INTO `recommend_record` VALUES (584, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'EMBEDDING', '8,20,9', 828, '2026-05-02 09:18:58');
INSERT INTO `recommend_record` VALUES (585, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'EMBEDDING', '20,10,8', 815, '2026-05-02 09:18:59');
INSERT INTO `recommend_record` VALUES (586, NULL, '想找面积大一点，生活配套好的房子', 'EMBEDDING', '8,3,4', 836, '2026-05-02 09:19:00');
INSERT INTO `recommend_record` VALUES (587, NULL, '想找三室，适合家庭居住，生活方便', 'EMBEDDING', '9,16,15', 826, '2026-05-02 09:19:00');
INSERT INTO `recommend_record` VALUES (588, NULL, '想找医院附近，有电梯，生活方便的两室', 'EMBEDDING', '19,20,14', 828, '2026-05-02 09:19:01');
INSERT INTO `recommend_record` VALUES (589, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'EMBEDDING', '20,10,9', 836, '2026-05-02 09:19:02');
INSERT INTO `recommend_record` VALUES (590, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'EMBEDDING', '3,4,11,5,12,18,15,6', 366, '2026-05-02 09:19:11');
INSERT INTO `recommend_record` VALUES (591, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'EMBEDDING', '11,18', 161, '2026-05-02 09:19:11');
INSERT INTO `recommend_record` VALUES (592, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'EMBEDDING', '12,4,11,5,3,15,17,18,8,9', 801, '2026-05-02 09:19:12');
INSERT INTO `recommend_record` VALUES (593, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'EMBEDDING', '5,15,11,4,3,18,12,6', 378, '2026-05-02 09:19:12');
INSERT INTO `recommend_record` VALUES (594, NULL, '学生租房，希望交通方便，价格不要太高', 'EMBEDDING', '11,4,12,18,3,7,20,5,9,15', 805, '2026-05-02 09:19:13');
INSERT INTO `recommend_record` VALUES (595, NULL, '预算不超过1200元，最好靠近学校或者大学', 'EMBEDDING', '4,11,18,15', 237, '2026-05-02 09:19:13');
INSERT INTO `recommend_record` VALUES (596, NULL, '想找交通方便，靠近公交站或者主路的房子', 'EMBEDDING', '7,12,4,6,20,14,11,10,18,2', 817, '2026-05-02 09:19:14');
INSERT INTO `recommend_record` VALUES (597, NULL, '希望房子靠近公交站，平时出行方便', 'EMBEDDING', '7,20,6,14,2,10,4,12,11,17', 797, '2026-05-02 09:19:15');
INSERT INTO `recommend_record` VALUES (598, NULL, '想找通勤方便，靠近主路或者车站的房子', 'EMBEDDING', '7,4,12,20,11,14,6,10,18,9', 785, '2026-05-02 09:19:16');
INSERT INTO `recommend_record` VALUES (599, NULL, '想找交通便利，同时房子最好是精装的公寓', 'EMBEDDING', '3,7,4,8,21,12,20,16,10,18', 786, '2026-05-02 09:19:16');
INSERT INTO `recommend_record` VALUES (600, NULL, '希望附近有公交站，周边生活方便', 'EMBEDDING', '6,7,14,20,2,10,17,19,4,11', 798, '2026-05-02 09:19:17');
INSERT INTO `recommend_record` VALUES (601, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'EMBEDDING', '20,10,9,4,14,6,12,7,11,3', 774, '2026-05-02 09:19:18');
INSERT INTO `recommend_record` VALUES (602, NULL, '想找精装修，可以直接入住的公寓', 'EMBEDDING', '3,21,4,7,16,18,8,13,12,17', 796, '2026-05-02 09:19:19');
INSERT INTO `recommend_record` VALUES (603, NULL, '希望房子装修好，能够拎包入住', 'EMBEDDING', '21,3,4,13,8,7,16,9,12,17', 842, '2026-05-02 09:19:20');
INSERT INTO `recommend_record` VALUES (604, NULL, '想找有电梯，精装，居住体验好一点的一室', 'EMBEDDING', '13,3,8,16,21,4,19,7,20,17', 787, '2026-05-02 09:19:21');
INSERT INTO `recommend_record` VALUES (605, NULL, '想找精装一室一厅，适合单人居住', 'EMBEDDING', '8,3,4,16,21,13,17,18,9,20', 839, '2026-05-02 09:19:21');
INSERT INTO `recommend_record` VALUES (606, NULL, '希望房子适合一个人住，装修不要太旧', 'EMBEDDING', '8,21,3,4,17,16,13,15,12,18', 826, '2026-05-02 09:19:22');
INSERT INTO `recommend_record` VALUES (607, NULL, '想找拎包入住的两室房源，装修新一点', 'EMBEDDING', '21,8,3,4,13,20,10,16,19,12', 832, '2026-05-02 09:19:23');
INSERT INTO `recommend_record` VALUES (608, NULL, '想找安静一点，适合考研复习的房子', 'EMBEDDING', '5,15,17,12,4,8,11,9,20,16', 856, '2026-05-02 09:19:24');
INSERT INTO `recommend_record` VALUES (609, NULL, '希望靠近学校，适合学习，环境不要太吵', 'EMBEDDING', '4,15,5,17,11,12,3,8,9,16', 849, '2026-05-02 09:19:25');
INSERT INTO `recommend_record` VALUES (610, NULL, '想找安静小区，晚上休息不受影响', 'EMBEDDING', '15,17,5,8,11,10,20,4,12,18', 850, '2026-05-02 09:19:26');
INSERT INTO `recommend_record` VALUES (611, NULL, '考研期间居住，希望房子安静，同时交通方便', 'EMBEDDING', '5,15,17,11,12,4,20,8,3,9', 842, '2026-05-02 09:19:27');
INSERT INTO `recommend_record` VALUES (612, NULL, '想找安静的一室房源，适合学习和休息', 'EMBEDDING', '15,5,17,11,12,8,4,3,10,18', 828, '2026-05-02 09:19:27');
INSERT INTO `recommend_record` VALUES (613, NULL, '希望学习环境好，房子最好精装，周边安静', 'EMBEDDING', '5,3,4,15,12,17,8,9,11,21', 822, '2026-05-02 09:19:28');
INSERT INTO `recommend_record` VALUES (614, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'EMBEDDING', '8,20,9,12,17,10,16,3,4,11', 835, '2026-05-02 09:19:29');
INSERT INTO `recommend_record` VALUES (615, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'EMBEDDING', '20,10,8,9,19,14,16,21,3,12', 848, '2026-05-02 09:19:30');
INSERT INTO `recommend_record` VALUES (616, NULL, '想找面积大一点，生活配套好的房子', 'EMBEDDING', '8,3,4,20,9,16,21,10,17,12', 857, '2026-05-02 09:19:31');
INSERT INTO `recommend_record` VALUES (617, NULL, '想找三室，适合家庭居住，生活方便', 'EMBEDDING', '9,16,15,8,11,3,12,20,4,17', 869, '2026-05-02 09:19:32');
INSERT INTO `recommend_record` VALUES (618, NULL, '想找医院附近，有电梯，生活方便的两室', 'EMBEDDING', '19,20,14,10,8,12,13,6,4,2', 848, '2026-05-02 09:19:33');
INSERT INTO `recommend_record` VALUES (619, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'EMBEDDING', '20,10,9,8,12,4,11,16,3,17', 867, '2026-05-02 09:19:33');
INSERT INTO `recommend_record` VALUES (620, NULL, '我想找一套1500元以内，靠近学校，适合考研复习的一室一厅', 'EMBEDDING', '5,12,3', 1385, '2026-05-02 09:42:59');
INSERT INTO `recommend_record` VALUES (621, NULL, '我想找交通方便，靠近公交站，精装修，可以直接入住的公寓', 'TFIDF', '3,7,21,4,13', 34, '2026-05-02 09:43:59');
INSERT INTO `recommend_record` VALUES (622, NULL, '我想找适合一家人住的两室一厅，生活方便，周边配套齐全', 'RULE', '21,20,19,10,8', 12, '2026-05-02 09:45:43');
INSERT INTO `recommend_record` VALUES (623, NULL, '我想找一套1500元以内，靠近学校，适合考研复习的一室一厅', 'EMBEDDING', '5,3,12', 3818, '2026-05-02 21:08:47');
INSERT INTO `recommend_record` VALUES (624, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'RULE', '11,5,12,4,3', 4290, '2026-05-02 21:10:00');
INSERT INTO `recommend_record` VALUES (625, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'RULE', '18,11', 7378, '2026-05-02 21:10:07');
INSERT INTO `recommend_record` VALUES (626, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'RULE', '4,18,6', 6954, '2026-05-02 21:10:14');
INSERT INTO `recommend_record` VALUES (627, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'RULE', '11,5,12,4,3', 3793, '2026-05-02 21:10:18');
INSERT INTO `recommend_record` VALUES (628, NULL, '学生租房，希望交通方便，价格不要太高', 'RULE', '12,11,4,3,20', 4574, '2026-05-02 21:10:22');
INSERT INTO `recommend_record` VALUES (629, NULL, '预算不超过1200元，最好靠近学校或者大学', 'RULE', '11,4,15,18', 4357, '2026-05-02 21:10:27');
INSERT INTO `recommend_record` VALUES (630, NULL, '想找交通方便，靠近公交站或者主路的房子', 'RULE', '20,19,18,14,12', 3929, '2026-05-02 21:10:31');
INSERT INTO `recommend_record` VALUES (631, NULL, '希望房子靠近公交站，平时出行方便', 'RULE', '20,19,18,14,12', 5335, '2026-05-02 21:10:36');
INSERT INTO `recommend_record` VALUES (632, NULL, '想找通勤方便，靠近主路或者车站的房子', 'RULE', '20,19,18,14,12', 3569, '2026-05-02 21:10:40');
INSERT INTO `recommend_record` VALUES (633, NULL, '想找交通便利，同时房子最好是精装的公寓', 'RULE', '18,12,7,4,3', 5770, '2026-05-02 21:10:46');
INSERT INTO `recommend_record` VALUES (634, NULL, '希望附近有公交站，周边生活方便', 'RULE', '20,19,6,18,14', 3567, '2026-05-02 21:10:49');
INSERT INTO `recommend_record` VALUES (635, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'RULE', '12,11,5,4,3', 3536, '2026-05-02 21:10:53');
INSERT INTO `recommend_record` VALUES (636, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'RULE', '20,19,6,18,14', 4778, '2026-05-02 21:10:54');
INSERT INTO `recommend_record` VALUES (637, NULL, '想找精装修，可以直接入住的公寓', 'RULE', '', 4891, '2026-05-02 21:10:59');
INSERT INTO `recommend_record` VALUES (638, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'RULE', '18,11', 8718, '2026-05-02 21:11:02');
INSERT INTO `recommend_record` VALUES (639, NULL, '希望房子装修好，能够拎包入住', 'RULE', '21,18,13,12,10', 3328, '2026-05-02 21:11:02');
INSERT INTO `recommend_record` VALUES (640, NULL, '想找有电梯，精装，居住体验好一点的一室', 'RULE', '13,12,3,17,5', 5354, '2026-05-02 21:11:08');
INSERT INTO `recommend_record` VALUES (641, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'RULE', '4,18,6', 8772, '2026-05-02 21:11:11');
INSERT INTO `recommend_record` VALUES (642, NULL, '想找精装一室一厅，适合单人居住', 'RULE', '13,12,3,17,5', 3623, '2026-05-02 21:11:11');
INSERT INTO `recommend_record` VALUES (643, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'RULE', '11,5,12,4,3', 2977, '2026-05-02 21:11:14');
INSERT INTO `recommend_record` VALUES (644, NULL, '希望房子适合一个人住，装修不要太旧', 'RULE', '18,13,12,7,4', 4837, '2026-05-02 21:11:16');
INSERT INTO `recommend_record` VALUES (645, NULL, '学生租房，希望交通方便，价格不要太高', 'RULE', '12,11,4,3,20', 2940, '2026-05-02 21:11:17');
INSERT INTO `recommend_record` VALUES (646, NULL, '预算不超过1200元，最好靠近学校或者大学', 'RULE', '11,4,18,15', 3985, '2026-05-02 21:11:21');
INSERT INTO `recommend_record` VALUES (647, NULL, '想找拎包入住的两室房源，装修新一点', 'RULE', '21,10,20,19,14', 6339, '2026-05-02 21:11:22');
INSERT INTO `recommend_record` VALUES (648, NULL, '想找交通方便，靠近公交站或者主路的房子', 'RULE', '20,19,18,14,12', 3181, '2026-05-02 21:11:24');
INSERT INTO `recommend_record` VALUES (649, NULL, '想找安静一点，适合考研复习的房子', 'RULE', '11,5,12,9,4', 3294, '2026-05-02 21:11:26');
INSERT INTO `recommend_record` VALUES (650, NULL, '希望房子靠近公交站，平时出行方便', 'RULE', '20,19,18,14,12', 3354, '2026-05-02 21:11:27');
INSERT INTO `recommend_record` VALUES (651, NULL, '希望靠近学校，适合学习，环境不要太吵', 'RULE', '11,5,12,9,4', 3543, '2026-05-02 21:11:29');
INSERT INTO `recommend_record` VALUES (652, NULL, '想找通勤方便，靠近主路或者车站的房子', 'RULE', '20,19,18,14,12', 3774, '2026-05-02 21:11:31');
INSERT INTO `recommend_record` VALUES (653, NULL, '想找安静小区，晚上休息不受影响', 'RULE', '11,5,12,9,4', 3970, '2026-05-02 21:11:33');
INSERT INTO `recommend_record` VALUES (654, NULL, '想找交通便利，同时房子最好是精装的公寓', 'RULE', '18,12,7,4,3', 3690, '2026-05-02 21:11:35');
INSERT INTO `recommend_record` VALUES (655, NULL, '考研期间居住，希望房子安静，同时交通方便', 'RULE', '11,12,4,3,5', 3849, '2026-05-02 21:11:37');
INSERT INTO `recommend_record` VALUES (656, NULL, '希望附近有公交站，周边生活方便', 'RULE', '20,19,6,18,14', 3650, '2026-05-02 21:11:38');
INSERT INTO `recommend_record` VALUES (657, NULL, '想找安静的一室房源，适合学习和休息', 'RULE', '5,12,3,17,13', 4498, '2026-05-02 21:11:42');
INSERT INTO `recommend_record` VALUES (658, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'RULE', '20,19,6,18,14', 5886, '2026-05-02 21:11:44');
INSERT INTO `recommend_record` VALUES (659, NULL, '希望学习环境好，房子最好精装，周边安静', 'RULE', '12,4,3,11,5', 5582, '2026-05-02 21:11:47');
INSERT INTO `recommend_record` VALUES (660, NULL, '想找精装修，可以直接入住的公寓', 'RULE', '', 3344, '2026-05-02 21:11:48');
INSERT INTO `recommend_record` VALUES (661, NULL, '希望房子装修好，能够拎包入住', 'RULE', '21,18,13,12,10', 3316, '2026-05-02 21:11:51');
INSERT INTO `recommend_record` VALUES (662, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'RULE', '21,20,19,16,10', 7442, '2026-05-02 21:11:55');
INSERT INTO `recommend_record` VALUES (663, NULL, '想找有电梯，精装，居住体验好一点的一室', 'RULE', '13,12,3,17,5', 5846, '2026-05-02 21:11:57');
INSERT INTO `recommend_record` VALUES (664, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'RULE', '20,19,14,2,21', 3627, '2026-05-02 21:11:58');
INSERT INTO `recommend_record` VALUES (665, NULL, '想找精装一室一厅，适合单人居住', 'RULE', '13,12,3,17,5', 5632, '2026-05-02 21:12:02');
INSERT INTO `recommend_record` VALUES (666, NULL, '想找面积大一点，生活配套好的房子', 'RULE', '21,20,19,16,10', 6470, '2026-05-02 21:12:05');
INSERT INTO `recommend_record` VALUES (667, NULL, '希望房子适合一个人住，装修不要太旧', 'RULE', '18,13,12,7,4', 3649, '2026-05-02 21:12:06');
INSERT INTO `recommend_record` VALUES (668, NULL, '想找拎包入住的两室房源，装修新一点', 'RULE', '21,10,20,19,14', 5538, '2026-05-02 21:12:12');
INSERT INTO `recommend_record` VALUES (669, NULL, '想找安静一点，适合考研复习的房子', 'RULE', '11,5,12,9,4', 2790, '2026-05-02 21:12:14');
INSERT INTO `recommend_record` VALUES (670, NULL, '想找三室，适合家庭居住，生活方便', 'RULE', '16,9', 11823, '2026-05-02 21:12:17');
INSERT INTO `recommend_record` VALUES (671, NULL, '希望靠近学校，适合学习，环境不要太吵', 'RULE', '11,5,12,9,4', 5938, '2026-05-02 21:12:20');
INSERT INTO `recommend_record` VALUES (672, NULL, '想找医院附近，有电梯，生活方便的两室', 'RULE', '20,19,14,2,21', 6618, '2026-05-02 21:12:23');
INSERT INTO `recommend_record` VALUES (673, NULL, '想找安静小区，晚上休息不受影响', 'RULE', '11,5,12,9,4', 4167, '2026-05-02 21:12:25');
INSERT INTO `recommend_record` VALUES (674, NULL, '考研期间居住，希望房子安静，同时交通方便', 'RULE', '11,12,4,3,5', 3234, '2026-05-02 21:12:28');
INSERT INTO `recommend_record` VALUES (675, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'RULE', '20,19,6,18,14', 10194, '2026-05-02 21:12:33');
INSERT INTO `recommend_record` VALUES (676, NULL, '想找安静的一室房源，适合学习和休息', 'RULE', '5,12,3,17,13', 7697, '2026-05-02 21:12:35');
INSERT INTO `recommend_record` VALUES (677, NULL, '希望学习环境好，房子最好精装，周边安静', 'RULE', '12,4,3,11,5', 2906, '2026-05-02 21:12:38');
INSERT INTO `recommend_record` VALUES (678, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'RULE', '20,19,6,18,14', 5947, '2026-05-02 21:12:44');
INSERT INTO `recommend_record` VALUES (679, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'TFIDF', '3,4,12', 5914, '2026-05-02 21:12:52');
INSERT INTO `recommend_record` VALUES (680, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'RULE', '21,20,19,10,8', 9373, '2026-05-02 21:12:54');
INSERT INTO `recommend_record` VALUES (681, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'TFIDF', '11', 6700, '2026-05-02 21:12:59');
INSERT INTO `recommend_record` VALUES (682, NULL, '想找面积大一点，生活配套好的房子', 'RULE', '21,20,19,16,10', 6021, '2026-05-02 21:13:00');
INSERT INTO `recommend_record` VALUES (683, NULL, '想找三室，适合家庭居住，生活方便', 'RULE', '16,9', 5509, '2026-05-02 21:13:05');
INSERT INTO `recommend_record` VALUES (684, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'TFIDF', '4,18,6', 7629, '2026-05-02 21:13:06');
INSERT INTO `recommend_record` VALUES (685, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'TFIDF', '5,15,4', 3029, '2026-05-02 21:13:09');
INSERT INTO `recommend_record` VALUES (686, NULL, '学生租房，希望交通方便，价格不要太高', 'TFIDF', '11,4,3', 3278, '2026-05-02 21:13:13');
INSERT INTO `recommend_record` VALUES (687, NULL, '想找医院附近，有电梯，生活方便的两室', 'RULE', '21,20,19,10,8', 9107, '2026-05-02 21:13:14');
INSERT INTO `recommend_record` VALUES (688, NULL, '预算不超过1200元，最好靠近学校或者大学', 'TFIDF', '11,4', 5049, '2026-05-02 21:13:18');
INSERT INTO `recommend_record` VALUES (689, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'RULE', '20,19,6,18,14', 4447, '2026-05-02 21:13:19');
INSERT INTO `recommend_record` VALUES (690, NULL, '想找交通方便，靠近公交站或者主路的房子', 'TFIDF', '14,7,6', 4222, '2026-05-02 21:13:22');
INSERT INTO `recommend_record` VALUES (691, NULL, '希望房子靠近公交站，平时出行方便', 'TFIDF', '14,7,6', 4358, '2026-05-02 21:13:26');
INSERT INTO `recommend_record` VALUES (692, NULL, '想找通勤方便，靠近主路或者车站的房子', 'TFIDF', '14,7,6', 2710, '2026-05-02 21:13:29');
INSERT INTO `recommend_record` VALUES (693, NULL, '想找交通便利，同时房子最好是精装的公寓', 'TFIDF', '7,3,21', 5502, '2026-05-02 21:13:34');
INSERT INTO `recommend_record` VALUES (694, NULL, '希望附近有公交站，周边生活方便', 'TFIDF', '6,14,7', 3267, '2026-05-02 21:13:38');
INSERT INTO `recommend_record` VALUES (695, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'TFIDF', '20,10,14', 5716, '2026-05-02 21:13:43');
INSERT INTO `recommend_record` VALUES (696, NULL, '想找精装修，可以直接入住的公寓', 'TFIDF', '21,3,13', 5056, '2026-05-02 21:13:49');
INSERT INTO `recommend_record` VALUES (697, NULL, '希望房子装修好，能够拎包入住', 'TFIDF', '21,13,3', 2412, '2026-05-02 21:13:51');
INSERT INTO `recommend_record` VALUES (698, NULL, '想找有电梯，精装，居住体验好一点的一室', 'TFIDF', '13,3,17', 4641, '2026-05-02 21:13:56');
INSERT INTO `recommend_record` VALUES (699, NULL, '想找精装一室一厅，适合单人居住', 'TFIDF', '13,3,17', 5185, '2026-05-02 21:14:01');
INSERT INTO `recommend_record` VALUES (700, NULL, '希望房子适合一个人住，装修不要太旧', 'TFIDF', '21,13,3', 4338, '2026-05-02 21:14:05');
INSERT INTO `recommend_record` VALUES (701, NULL, '想找拎包入住的两室房源，装修新一点', 'TFIDF', '21,2,8', 5138, '2026-05-02 21:14:10');
INSERT INTO `recommend_record` VALUES (702, NULL, '想找安静一点，适合考研复习的房子', 'TFIDF', '5,15,17', 3990, '2026-05-02 21:14:14');
INSERT INTO `recommend_record` VALUES (703, NULL, '希望靠近学校，适合学习，环境不要太吵', 'TFIDF', '5,15,3', 4809, '2026-05-02 21:14:19');
INSERT INTO `recommend_record` VALUES (704, NULL, '想找安静小区，晚上休息不受影响', 'TFIDF', '15,5,17', 5715, '2026-05-02 21:14:25');
INSERT INTO `recommend_record` VALUES (705, NULL, '考研期间居住，希望房子安静，同时交通方便', 'TFIDF', '5,15,4', 3413, '2026-05-02 21:14:28');
INSERT INTO `recommend_record` VALUES (706, NULL, '想找安静的一室房源，适合学习和休息', 'TFIDF', '5,15,17', 5864, '2026-05-02 21:14:34');
INSERT INTO `recommend_record` VALUES (707, NULL, '希望学习环境好，房子最好精装，周边安静', 'TFIDF', '5,3,4', 4437, '2026-05-02 21:14:39');
INSERT INTO `recommend_record` VALUES (708, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'TFIDF', '8,19,16', 6289, '2026-05-02 21:14:45');
INSERT INTO `recommend_record` VALUES (709, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'TFIDF', '20,10,14', 5995, '2026-05-02 21:14:51');
INSERT INTO `recommend_record` VALUES (710, NULL, '想找面积大一点，生活配套好的房子', 'TFIDF', '8,16,21', 5657, '2026-05-02 21:14:57');
INSERT INTO `recommend_record` VALUES (711, NULL, '想找三室，适合家庭居住，生活方便', 'TFIDF', '16,9', 8413, '2026-05-02 21:15:05');
INSERT INTO `recommend_record` VALUES (712, NULL, '想找医院附近，有电梯，生活方便的两室', 'TFIDF', '19,8,10', 11398, '2026-05-02 21:15:16');
INSERT INTO `recommend_record` VALUES (713, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'TFIDF', '8,10,20', 12220, '2026-05-02 21:15:29');
INSERT INTO `recommend_record` VALUES (714, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'TFIDF', '3,4,11', 28, '2026-05-02 22:18:35');
INSERT INTO `recommend_record` VALUES (715, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'TFIDF', '18', 11, '2026-05-02 22:18:35');
INSERT INTO `recommend_record` VALUES (716, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'TFIDF', '3,12,5', 9, '2026-05-02 22:18:35');
INSERT INTO `recommend_record` VALUES (717, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'TFIDF', '5,15,18', 20, '2026-05-02 22:18:35');
INSERT INTO `recommend_record` VALUES (718, NULL, '学生租房，希望交通方便，价格不要太高', 'TFIDF', '11,4,3', 18, '2026-05-02 22:18:35');
INSERT INTO `recommend_record` VALUES (719, NULL, '预算不超过1200元，最好靠近学校或者大学', 'TFIDF', '11,4', 5, '2026-05-02 22:18:35');
INSERT INTO `recommend_record` VALUES (720, NULL, '想找交通方便，靠近公交站或者主路的房子', 'TFIDF', '14,7,6', 12, '2026-05-02 22:18:35');
INSERT INTO `recommend_record` VALUES (721, NULL, '希望房子靠近公交站，平时出行方便', 'TFIDF', '14,7,6', 11, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (722, NULL, '想找通勤方便，靠近主路或者车站的房子', 'TFIDF', '14,7,3', 10, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (723, NULL, '想找交通便利，同时房子最好是精装的公寓', 'TFIDF', '7,3,21', 10, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (724, NULL, '希望附近有公交站，周边生活方便', 'TFIDF', '8,19,16', 12, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (725, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'TFIDF', '20,10,14', 9, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (726, NULL, '想找精装修，可以直接入住的公寓', 'TFIDF', '21,3,13', 8, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (727, NULL, '希望房子装修好，能够拎包入住', 'TFIDF', '21,4,13', 7, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (728, NULL, '想找有电梯，精装，居住体验好一点的一室', 'TFIDF', '13,3,7', 4, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (729, NULL, '想找精装一室一厅，适合单人居住', 'TFIDF', '13,3,17', 4, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (730, NULL, '希望房子适合一个人住，装修不要太旧', 'TFIDF', '21,13,3', 6, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (731, NULL, '想找拎包入住的两室房源，装修新一点', 'TFIDF', '21,8,19', 4, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (732, NULL, '想找安静一点，适合考研复习的房子', 'TFIDF', '5,15,17', 7, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (733, NULL, '希望靠近学校，适合学习，环境不要太吵', 'TFIDF', '5,15,3', 7, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (734, NULL, '想找安静小区，晚上休息不受影响', 'TFIDF', '15,5,17', 7, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (735, NULL, '考研期间居住，希望房子安静，同时交通方便', 'TFIDF', '5,15,17', 7, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (736, NULL, '想找安静的一室房源，适合学习和休息', 'TFIDF', '5,17,12', 5, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (737, NULL, '希望学习环境好，房子最好精装，周边安静', 'TFIDF', '5,21,15', 9, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (738, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'TFIDF', '8,19,16', 7, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (739, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'TFIDF', '8,10,20', 3, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (740, NULL, '想找面积大一点，生活配套好的房子', 'TFIDF', '8,16,19', 6, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (741, NULL, '想找三室，适合家庭居住，生活方便', 'TFIDF', '16,9', 3, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (742, NULL, '想找医院附近，有电梯，生活方便的两室', 'TFIDF', '21,19,8', 5, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (743, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'TFIDF', '8,10,20', 6, '2026-05-02 22:18:36');
INSERT INTO `recommend_record` VALUES (744, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'RULE', '12,11,5,4,3', 5, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (745, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'RULE', '18', 3, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (746, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'RULE', '12,5,3,17,13', 5, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (747, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'RULE', '11,5,12,4,3', 3, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (748, NULL, '学生租房，希望交通方便，价格不要太高', 'RULE', '12,11,4,3,20', 3, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (749, NULL, '预算不超过1200元，最好靠近学校或者大学', 'RULE', '11,4,18,15', 2, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (750, NULL, '想找交通方便，靠近公交站或者主路的房子', 'RULE', '20,19,18,14,12', 6, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (751, NULL, '希望房子靠近公交站，平时出行方便', 'RULE', '20,19,18,14,12', 3, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (752, NULL, '想找通勤方便，靠近主路或者车站的房子', 'RULE', '20,19,18,14,12', 5, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (753, NULL, '想找交通便利，同时房子最好是精装的公寓', 'RULE', '18,12,7,4,3', 4, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (754, NULL, '希望附近有公交站，周边生活方便', 'RULE', '20,19,6,18,14', 4, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (755, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'RULE', '20,19,6,18,14', 4, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (756, NULL, '想找精装修，可以直接入住的公寓', 'RULE', '21,18,13,12,10', 4, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (757, NULL, '希望房子装修好，能够拎包入住', 'RULE', '21,18,13,12,10', 3, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (758, NULL, '想找有电梯，精装，居住体验好一点的一室', 'RULE', '13,12,7,3,17', 4, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (759, NULL, '想找精装一室一厅，适合单人居住', 'RULE', '13,12,3,17,5', 4, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (760, NULL, '希望房子适合一个人住，装修不要太旧', 'RULE', '18,13,12,7,4', 5, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (761, NULL, '想找拎包入住的两室房源，装修新一点', 'RULE', '21,10,20,19,8', 3, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (762, NULL, '想找安静一点，适合考研复习的房子', 'RULE', '11,5,12,9,4', 4, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (763, NULL, '希望靠近学校，适合学习，环境不要太吵', 'RULE', '11,5,12,9,4', 3, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (764, NULL, '想找安静小区，晚上休息不受影响', 'RULE', '11,5,12,9,4', 4, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (765, NULL, '考研期间居住，希望房子安静，同时交通方便', 'RULE', '11,12,4,3,5', 4, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (766, NULL, '想找安静的一室房源，适合学习和休息', 'RULE', '5,12,3,17,13', 2, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (767, NULL, '希望学习环境好，房子最好精装，周边安静', 'RULE', '12,4,3,11,5', 2, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (768, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'RULE', '21,20,19,16,10', 3, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (769, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'RULE', '21,20,19,10,8', 3, '2026-05-02 22:19:03');
INSERT INTO `recommend_record` VALUES (770, NULL, '想找面积大一点，生活配套好的房子', 'RULE', '21,20,19,16,10', 2, '2026-05-02 22:19:04');
INSERT INTO `recommend_record` VALUES (771, NULL, '想找三室，适合家庭居住，生活方便', 'RULE', '16,9', 4, '2026-05-02 22:19:04');
INSERT INTO `recommend_record` VALUES (772, NULL, '想找医院附近，有电梯，生活方便的两室', 'RULE', '21,10,20,19,8', 2, '2026-05-02 22:19:04');
INSERT INTO `recommend_record` VALUES (773, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'RULE', '21,20,19,16,10', 3, '2026-05-02 22:19:04');
INSERT INTO `recommend_record` VALUES (774, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'TFIDF', '3,4,11,12,5', 3, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (775, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'TFIDF', '18', 2, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (776, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'TFIDF', '3,12,5,17,13', 3, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (777, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'TFIDF', '5,15,18,3,12', 4, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (778, NULL, '学生租房，希望交通方便，价格不要太高', 'TFIDF', '11,4,3,14,12', 8, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (779, NULL, '预算不超过1200元，最好靠近学校或者大学', 'TFIDF', '11,4', 2, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (780, NULL, '想找交通方便，靠近公交站或者主路的房子', 'TFIDF', '14,7,6,3,4', 5, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (781, NULL, '希望房子靠近公交站，平时出行方便', 'TFIDF', '14,7,6,20,4', 6, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (782, NULL, '想找通勤方便，靠近主路或者车站的房子', 'TFIDF', '14,7,3,6,4', 5, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (783, NULL, '想找交通便利，同时房子最好是精装的公寓', 'TFIDF', '7,3,21,4,13', 5, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (784, NULL, '希望附近有公交站，周边生活方便', 'TFIDF', '8,19,16,6,14', 7, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (785, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'TFIDF', '20,10,14,7,6', 5, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (786, NULL, '想找精装修，可以直接入住的公寓', 'TFIDF', '21,3,13,4,7', 6, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (787, NULL, '希望房子装修好，能够拎包入住', 'TFIDF', '21,4,13,3,7', 5, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (788, NULL, '想找有电梯，精装，居住体验好一点的一室', 'TFIDF', '13,3,7,17,12', 3, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (789, NULL, '想找精装一室一厅，适合单人居住', 'TFIDF', '13,3,17,12,5', 4, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (790, NULL, '希望房子适合一个人住，装修不要太旧', 'TFIDF', '21,13,3,4,7', 7, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (791, NULL, '想找拎包入住的两室房源，装修新一点', 'TFIDF', '21,8,19,10,20', 5, '2026-05-02 22:19:13');
INSERT INTO `recommend_record` VALUES (792, NULL, '想找安静一点，适合考研复习的房子', 'TFIDF', '5,15,17,14,13', 6, '2026-05-02 22:19:14');
INSERT INTO `recommend_record` VALUES (793, NULL, '希望靠近学校，适合学习，环境不要太吵', 'TFIDF', '5,15,3,4,11', 6, '2026-05-02 22:19:14');
INSERT INTO `recommend_record` VALUES (794, NULL, '想找安静小区，晚上休息不受影响', 'TFIDF', '15,5,17', 5, '2026-05-02 22:19:14');
INSERT INTO `recommend_record` VALUES (795, NULL, '考研期间居住，希望房子安静，同时交通方便', 'TFIDF', '5,15,17,14,7', 5, '2026-05-02 22:19:14');
INSERT INTO `recommend_record` VALUES (796, NULL, '想找安静的一室房源，适合学习和休息', 'TFIDF', '5,17,12,3,13', 3, '2026-05-02 22:19:14');
INSERT INTO `recommend_record` VALUES (797, NULL, '希望学习环境好，房子最好精装，周边安静', 'TFIDF', '5,21,15,13,3', 5, '2026-05-02 22:19:14');
INSERT INTO `recommend_record` VALUES (798, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'TFIDF', '8,19,16,9,21', 5, '2026-05-02 22:19:14');
INSERT INTO `recommend_record` VALUES (799, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'TFIDF', '8,10,20,19,21', 4, '2026-05-02 22:19:14');
INSERT INTO `recommend_record` VALUES (800, NULL, '想找面积大一点，生活配套好的房子', 'TFIDF', '8,16,19,9,10', 4, '2026-05-02 22:19:14');
INSERT INTO `recommend_record` VALUES (801, NULL, '想找三室，适合家庭居住，生活方便', 'TFIDF', '16,9', 3, '2026-05-02 22:19:14');
INSERT INTO `recommend_record` VALUES (802, NULL, '想找医院附近，有电梯，生活方便的两室', 'TFIDF', '21,19,8,10,20', 4, '2026-05-02 22:19:14');
INSERT INTO `recommend_record` VALUES (803, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'TFIDF', '8,10,20,19,9', 4, '2026-05-02 22:19:14');
INSERT INTO `recommend_record` VALUES (804, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'TFIDF', '3,4,11,12,5,15,18,6', 4, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (805, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'TFIDF', '18', 2, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (806, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'TFIDF', '3,12,5,17,13,7', 4, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (807, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'TFIDF', '5,15,18,3,12,6,4,11', 4, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (808, NULL, '学生租房，希望交通方便，价格不要太高', 'TFIDF', '11,4,3,14,12,7,6,2,5,18', 8, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (809, NULL, '预算不超过1200元，最好靠近学校或者大学', 'TFIDF', '11,4', 3, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (810, NULL, '想找交通方便，靠近公交站或者主路的房子', 'TFIDF', '14,7,6,3,4,2,19,18,11,12', 5, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (811, NULL, '希望房子靠近公交站，平时出行方便', 'TFIDF', '14,7,6,20,4,19,2,12,17,3', 10, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (812, NULL, '想找通勤方便，靠近主路或者车站的房子', 'TFIDF', '14,7,3,6,4,2,19,12,20,18', 5, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (813, NULL, '想找交通便利，同时房子最好是精装的公寓', 'TFIDF', '7,3,21,4,13,14,18,20,6,12', 5, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (814, NULL, '希望附近有公交站，周边生活方便', 'TFIDF', '8,19,16,6,14,20,9,21,7,10', 5, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (815, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'TFIDF', '20,10,14,7,6,4,9,2,3,18', 5, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (816, NULL, '想找精装修，可以直接入住的公寓', 'TFIDF', '21,3,13,4,7,18,12,10', 5, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (817, NULL, '希望房子装修好，能够拎包入住', 'TFIDF', '21,4,13,3,7,18', 4, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (818, NULL, '想找有电梯，精装，居住体验好一点的一室', 'TFIDF', '13,3,7,17,12,5', 4, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (819, NULL, '想找精装一室一厅，适合单人居住', 'TFIDF', '13,3,17,12,5', 4, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (820, NULL, '希望房子适合一个人住，装修不要太旧', 'TFIDF', '21,13,3,4,7,8,18,14,20,17', 3, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (821, NULL, '想找拎包入住的两室房源，装修新一点', 'TFIDF', '21,8,19,10,20,14,2', 2, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (822, NULL, '想找安静一点，适合考研复习的房子', 'TFIDF', '5,15,17,14,13,8,3,20,18,12', 4, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (823, NULL, '希望靠近学校，适合学习，环境不要太吵', 'TFIDF', '5,15,3,4,11,17,12,9,14,20', 4, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (824, NULL, '想找安静小区，晚上休息不受影响', 'TFIDF', '15,5,17', 4, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (825, NULL, '考研期间居住，希望房子安静，同时交通方便', 'TFIDF', '5,15,17,14,7,6,4,20,2,3', 5, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (826, NULL, '想找安静的一室房源，适合学习和休息', 'TFIDF', '5,17,12,3,13,7', 3, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (827, NULL, '希望学习环境好，房子最好精装，周边安静', 'TFIDF', '5,21,15,13,3,4,17,7,18,8', 5, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (828, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'TFIDF', '8,19,16,9,21,10,20,6,14,2', 4, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (829, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'TFIDF', '8,10,20,19,21,2,14', 2, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (830, NULL, '想找面积大一点，生活配套好的房子', 'TFIDF', '8,16,19,9,10,21,6,20,2,14', 5, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (831, NULL, '想找三室，适合家庭居住，生活方便', 'TFIDF', '16,9', 2, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (832, NULL, '想找医院附近，有电梯，生活方便的两室', 'TFIDF', '21,19,8,10,20,14,2', 4, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (833, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'TFIDF', '8,10,20,19,9,16,21,3,6,14', 7, '2026-05-02 22:19:18');
INSERT INTO `recommend_record` VALUES (834, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'RULE', '12,11,5,4,3,18,15,6', 4, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (835, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'RULE', '18', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (836, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'RULE', '12,5,3,17,13,7', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (837, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'RULE', '11,5,12,4,3,15,18,6', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (838, NULL, '学生租房，希望交通方便，价格不要太高', 'RULE', '12,11,4,3,20,19,18,14,9,7', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (839, NULL, '预算不超过1200元，最好靠近学校或者大学', 'RULE', '11,4,18,15', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (840, NULL, '想找交通方便，靠近公交站或者主路的房子', 'RULE', '20,19,18,14,12,11,7,6,4,3', 3, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (841, NULL, '希望房子靠近公交站，平时出行方便', 'RULE', '20,19,18,14,12,11,7,6,4,3', 3, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (842, NULL, '想找通勤方便，靠近主路或者车站的房子', 'RULE', '20,19,18,14,12,11,7,6,4,3', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (843, NULL, '想找交通便利，同时房子最好是精装的公寓', 'RULE', '18,12,7,4,3,20,19,14,11,6', 3, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (844, NULL, '希望附近有公交站，周边生活方便', 'RULE', '20,19,6,18,14,12,11,7,4,3', 3, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (845, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'RULE', '20,19,6,18,14,12,11,7,4,3', 3, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (846, NULL, '想找精装修，可以直接入住的公寓', 'RULE', '21,18,13,12,10,7,4,3', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (847, NULL, '希望房子装修好，能够拎包入住', 'RULE', '21,18,13,12,10,7,4,3', 3, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (848, NULL, '想找有电梯，精装，居住体验好一点的一室', 'RULE', '13,12,7,3,17,5', 3, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (849, NULL, '想找精装一室一厅，适合单人居住', 'RULE', '13,12,3,17,5', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (850, NULL, '希望房子适合一个人住，装修不要太旧', 'RULE', '18,13,12,7,4,3,21,17,15,11', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (851, NULL, '想找拎包入住的两室房源，装修新一点', 'RULE', '21,10,20,19,8,14,2', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (852, NULL, '想找安静一点，适合考研复习的房子', 'RULE', '11,5,12,9,4,3,17,15', 3, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (853, NULL, '希望靠近学校，适合学习，环境不要太吵', 'RULE', '11,5,12,9,4,3,17,15', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (854, NULL, '想找安静小区，晚上休息不受影响', 'RULE', '11,5,12,9,4,3,17,15', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (855, NULL, '考研期间居住，希望房子安静，同时交通方便', 'RULE', '11,12,4,3,5,20,19,18,14,9', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (856, NULL, '想找安静的一室房源，适合学习和休息', 'RULE', '5,12,3,17,13,7', 3, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (857, NULL, '希望学习环境好，房子最好精装，周边安静', 'RULE', '12,4,3,11,5,9,21,18,13,10', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (858, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'RULE', '21,20,19,16,10,9,8,6', 3, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (859, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'RULE', '21,20,19,10,8,14,2', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (860, NULL, '想找面积大一点，生活配套好的房子', 'RULE', '21,20,19,16,10,9,8,6', 3, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (861, NULL, '想找三室，适合家庭居住，生活方便', 'RULE', '16,9', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (862, NULL, '想找医院附近，有电梯，生活方便的两室', 'RULE', '21,10,20,19,8,14,2', 3, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (863, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'RULE', '21,20,19,16,10,9,8,6', 2, '2026-05-02 22:19:23');
INSERT INTO `recommend_record` VALUES (864, NULL, '我想找一套1500元以内，适合学生住，最好靠近学校的房子', 'EMBEDDING', '3,4,12,11,5,18,15,6', 1003, '2026-05-02 22:19:32');
INSERT INTO `recommend_record` VALUES (865, NULL, '预算不超过1000元，想找低预算合租主卧或者单间', 'EMBEDDING', '18', 182, '2026-05-02 22:19:32');
INSERT INTO `recommend_record` VALUES (866, NULL, '想找大学附近，适合一个人住的单间或一室房源', 'EMBEDDING', '12,3,5,17,7,13', 514, '2026-05-02 22:19:33');
INSERT INTO `recommend_record` VALUES (867, NULL, '想找1500元以内，环境安静，适合考研复习的房子', 'EMBEDDING', '5,15,11,4,18,3,12,6', 680, '2026-05-02 22:19:34');
INSERT INTO `recommend_record` VALUES (868, NULL, '学生租房，希望交通方便，价格不要太高', 'EMBEDDING', '11,12,4,7,18,3,6,19,9,20', 1460, '2026-05-02 22:19:35');
INSERT INTO `recommend_record` VALUES (869, NULL, '预算不超过1200元，最好靠近学校或者大学', 'EMBEDDING', '4,11,18,15', 435, '2026-05-02 22:19:36');
INSERT INTO `recommend_record` VALUES (870, NULL, '想找交通方便，靠近公交站或者主路的房子', 'EMBEDDING', '7,6,14,20,12,4,2,11,19,10', 1410, '2026-05-02 22:19:37');
INSERT INTO `recommend_record` VALUES (871, NULL, '希望房子靠近公交站，平时出行方便', 'EMBEDDING', '7,14,6,2,20,4,12,10,11,17', 1449, '2026-05-02 22:19:38');
INSERT INTO `recommend_record` VALUES (872, NULL, '想找通勤方便，靠近主路或者车站的房子', 'EMBEDDING', '7,14,6,20,4,12,11,2,19,10', 1546, '2026-05-02 22:19:40');
INSERT INTO `recommend_record` VALUES (873, NULL, '想找交通便利，同时房子最好是精装的公寓', 'EMBEDDING', '7,3,21,4,13,6,12,20,18,8', 1452, '2026-05-02 22:19:41');
INSERT INTO `recommend_record` VALUES (874, NULL, '希望附近有公交站，周边生活方便', 'EMBEDDING', '20,6,14,7,8,10,19,9,17,11', 1474, '2026-05-02 22:19:43');
INSERT INTO `recommend_record` VALUES (875, NULL, '想找靠近商圈，交通方便，购物也方便的房子', 'EMBEDDING', '20,10,14,6,7,4,19,9,12,2', 1503, '2026-05-02 22:19:45');
INSERT INTO `recommend_record` VALUES (876, NULL, '想找精装修，可以直接入住的公寓', 'EMBEDDING', '21,3,13,7,4,16,8,18,12,17', 1569, '2026-05-02 22:19:46');
INSERT INTO `recommend_record` VALUES (877, NULL, '希望房子装修好，能够拎包入住', 'EMBEDDING', '21,13,3,4,7,8,16,9,12,18', 1652, '2026-05-02 22:19:48');
INSERT INTO `recommend_record` VALUES (878, NULL, '想找有电梯，精装，居住体验好一点的一室', 'EMBEDDING', '13,3,7,17,12,5', 606, '2026-05-02 22:19:48');
INSERT INTO `recommend_record` VALUES (879, NULL, '想找精装一室一厅，适合单人居住', 'EMBEDDING', '3,13,17,12,5', 577, '2026-05-02 22:19:49');
INSERT INTO `recommend_record` VALUES (880, NULL, '希望房子适合一个人住，装修不要太旧', 'EMBEDDING', '21,13,3,8,4,7,12,16,18,17', 1624, '2026-05-02 22:19:51');
INSERT INTO `recommend_record` VALUES (881, NULL, '想找拎包入住的两室房源，装修新一点', 'EMBEDDING', '21,8,10,20,19,14,2', 682, '2026-05-02 22:19:51');
INSERT INTO `recommend_record` VALUES (882, NULL, '想找安静一点，适合考研复习的房子', 'EMBEDDING', '5,15,17,4,11,12,8,9,3,20', 1565, '2026-05-02 22:19:53');
INSERT INTO `recommend_record` VALUES (883, NULL, '希望靠近学校，适合学习，环境不要太吵', 'EMBEDDING', '5,15,4,17,12,11,3,9,8,18', 1527, '2026-05-02 22:19:54');
INSERT INTO `recommend_record` VALUES (884, NULL, '想找安静小区，晚上休息不受影响', 'EMBEDDING', '5,15,17,11,4,12,10,8,18,20', 1511, '2026-05-02 22:19:56');
INSERT INTO `recommend_record` VALUES (885, NULL, '考研期间居住，希望房子安静，同时交通方便', 'EMBEDDING', '5,15,4,17,12,11,7,20,14,3', 1515, '2026-05-02 22:19:57');
INSERT INTO `recommend_record` VALUES (886, NULL, '想找安静的一室房源，适合学习和休息', 'EMBEDDING', '5,17,12,3,13,7', 670, '2026-05-02 22:19:58');
INSERT INTO `recommend_record` VALUES (887, NULL, '希望学习环境好，房子最好精装，周边安静', 'EMBEDDING', '5,4,3,21,15,17,7,13,12,8', 1634, '2026-05-02 22:20:00');
INSERT INTO `recommend_record` VALUES (888, NULL, '想找适合一家人住，生活方便，周边配套齐全的房子', 'EMBEDDING', '8,9,20,16,10,17,21,12,19,15', 1415, '2026-05-02 22:20:01');
INSERT INTO `recommend_record` VALUES (889, NULL, '想找两室一厅，靠近商圈，购物生活方便', 'EMBEDDING', '20,8,10,14,19,21,2', 611, '2026-05-02 22:20:02');
INSERT INTO `recommend_record` VALUES (890, NULL, '想找面积大一点，生活配套好的房子', 'EMBEDDING', '8,9,16,20,21,10,3,4,12,17', 1370, '2026-05-02 22:20:03');
INSERT INTO `recommend_record` VALUES (891, NULL, '想找三室，适合家庭居住，生活方便', 'EMBEDDING', '9,16', 306, '2026-05-02 22:20:04');
INSERT INTO `recommend_record` VALUES (892, NULL, '想找医院附近，有电梯，生活方便的两室', 'EMBEDDING', '19,21,8,20,10,14,2', 607, '2026-05-02 22:20:04');
INSERT INTO `recommend_record` VALUES (893, NULL, '想找购物方便，靠近商圈，适合一家人住的房子', 'EMBEDDING', '20,10,9,8,19,6,14,16,12,4', 1400, '2026-05-02 22:20:06');

-- ----------------------------
-- Table structure for rental_application
-- ----------------------------
DROP TABLE IF EXISTS `rental_application`;
CREATE TABLE `rental_application`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `house_id` bigint(20) NOT NULL COMMENT '房源ID',
  `tenant_id` bigint(20) NOT NULL COMMENT '租客ID',
  `landlord_id` bigint(20) NOT NULL COMMENT '出租者ID',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '状态：0待处理 1已同意 2已拒绝',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rental_application_house_id`(`house_id` ASC) USING BTREE,
  INDEX `idx_rental_application_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_rental_application_landlord_id`(`landlord_id` ASC) USING BTREE,
  CONSTRAINT `fk_rental_application_house` FOREIGN KEY (`house_id`) REFERENCES `house` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_rental_application_landlord` FOREIGN KEY (`landlord_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_rental_application_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租房申请表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of rental_application
-- ----------------------------
INSERT INTO `rental_application` VALUES (6, 2, 3, 2, 2, '', '2026-03-16 19:24:13', '2026-03-16 19:24:13');
INSERT INTO `rental_application` VALUES (7, 1, 3, 2, 1, '', '2026-03-16 19:24:49', '2026-03-16 19:24:49');
INSERT INTO `rental_application` VALUES (8, 2, 3, 2, 1, '', '2026-04-24 21:41:48', '2026-04-24 21:41:48');

-- ----------------------------
-- Table structure for repair_request
-- ----------------------------
DROP TABLE IF EXISTS `repair_request`;
CREATE TABLE `repair_request`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `house_id` bigint(20) NOT NULL COMMENT '房源ID',
  `tenant_id` bigint(20) NOT NULL COMMENT '租客ID',
  `landlord_id` bigint(20) NOT NULL COMMENT '出租者ID',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '报修内容',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '状态：0待处理 1处理中 2已完成',
  `result` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '处理结果',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_repair_request_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_repair_request_landlord_id`(`landlord_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '报修表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of repair_request
-- ----------------------------
INSERT INTO `repair_request` VALUES (1, 4, 3, 2, '卫生间水龙头漏水，需要尽快维修', 1, '已联系维修师傅，今天下午上门处理', '2026-03-15 15:52:51', '2026-03-15 15:52:51');
INSERT INTO `repair_request` VALUES (2, 1, 3, 4, '水龙头坏了', 0, NULL, '2026-03-16 17:36:15', '2026-03-16 17:36:15');
INSERT INTO `repair_request` VALUES (3, 4, 3, 2, '空调坏了', 1, '受到，已经派师傅去处理了\n', '2026-03-16 18:11:10', '2026-03-16 18:11:10');
INSERT INTO `repair_request` VALUES (4, 4, 3, 2, '床坏了', 1, '明天去修', '2026-03-17 22:25:55', '2026-03-17 22:25:55');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  `role_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码：ADMIN/LANDLORD/TENANT',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$4MeKRKsGyM.WlUawnvRhTO4dUI2sAtJOPhDQ9ED/vgPou0UwehFDm', '管理员', '13800000001', 'admin@test.com', NULL, 'ADMIN', 1, '2026-03-15 11:16:21', '2026-03-15 11:16:21');
INSERT INTO `sys_user` VALUES (2, 'landlord1', '$2a$10$fWTmQFOkLOg28u96vz2D8eWFgaUWK00M61Lk9yON44Rn1ynx7i4cy', '出租者A', '13800000002', 'landlord@test.com', NULL, 'LANDLORD', 1, '2026-03-15 11:16:21', '2026-03-15 11:16:21');
INSERT INTO `sys_user` VALUES (3, 'tenant1', '$2a$10$a0b3kOx4Jr0AHQWpXkXHO.03adsZ2CYtFOg0hUGMui4C5lk2fqHHi', '租客A', '13800000003', 'tenant@test.com', NULL, 'TENANT', 1, '2026-03-15 11:16:21', '2026-03-15 11:16:21');
INSERT INTO `sys_user` VALUES (4, 'tenant2', '123456', '租客B', '13800000004', 'tenant2@test.com', NULL, 'TENANT', 1, '2026-03-15 11:55:09', '2026-03-15 11:55:09');
INSERT INTO `sys_user` VALUES (5, 'tenant3', '123456', '租客C', '13800000005', 'tenant3@test.com', NULL, 'TENANT', 1, '2026-03-15 11:55:52', '2026-03-15 11:55:52');
INSERT INTO `sys_user` VALUES (6, 'tenant5', '123456', '租客e', '13800000007', 'tenant5@test.com', NULL, 'TENANT', 1, '2026-03-15 21:53:25', '2026-03-15 21:53:25');

-- ----------------------------
-- Table structure for user_rental_preference
-- ----------------------------
DROP TABLE IF EXISTS `user_rental_preference`;
CREATE TABLE `user_rental_preference` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '租客用户ID',
  `budget_min` decimal(10, 2) NULL DEFAULT NULL COMMENT '月租最低预算',
  `budget_max` decimal(10, 2) NULL DEFAULT NULL COMMENT '月租最高预算',
  `preferred_city` varchar(50) NULL DEFAULT NULL COMMENT '偏好城市',
  `preferred_area` varchar(100) NULL DEFAULT NULL COMMENT '偏好区域',
  `preferred_house_type` varchar(50) NULL DEFAULT NULL COMMENT '偏好户型',
  `workplace` varchar(150) NULL DEFAULT NULL COMMENT '工作或学习地点',
  `max_commute_minutes` int(11) NULL DEFAULT NULL COMMENT '最大接受通勤时间（分钟）',
  `preference_tags` varchar(500) NULL DEFAULT NULL COMMENT '居住偏好标签',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_rental_preference_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户长期租房偏好表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
