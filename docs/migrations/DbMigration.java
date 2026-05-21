import java.sql.*;

public class DbMigration {
    static String DB_PATH = "/opt/hotelmanage/data/linxi.db";

    public static void main(String[] args) throws Exception {
        String url = "jdbc:sqlite:" + DB_PATH;
        Class.forName("org.sqlite.JDBC");

        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();

            System.out.println("=== 生产数据库迁移开始 ===");

            int d1 = stmt.executeUpdate("DELETE FROM sys_role_menu WHERE menu_id IN (12, 42)");
            System.out.println("1. 删除菜单12/42角色关联: " + d1 + " 行");

            int d2 = stmt.executeUpdate("DELETE FROM sys_menu WHERE id IN (12, 42)");
            System.out.println("2. 删除菜单12/42: " + d2 + " 行");

            int u1 = stmt.executeUpdate("UPDATE sys_menu SET parent_id = 10, sort_no = 2 WHERE id = 41");
            System.out.println("3. 投资者列表(41)挪到门店管理(10)下: " + u1 + " 行");

            int d3 = stmt.executeUpdate("DELETE FROM sys_role_menu WHERE menu_id = 40");
            System.out.println("4. 删除菜单40角色关联: " + d3 + " 行");

            int d4 = stmt.executeUpdate("DELETE FROM sys_menu WHERE id = 40");
            System.out.println("5. 删除投资者管理菜单(40): " + d4 + " 行");

            int u2 = stmt.executeUpdate("UPDATE investor_store SET investment_ratio = investment_ratio * 100 WHERE investment_ratio < 1 AND investment_ratio > 0");
            System.out.println("6. 投资比例小数→百分比: " + u2 + " 行");

            conn.commit();
            System.out.println("\n=== 迁移已提交 ===");

            System.out.println("\n=== 验证 ===");

            ResultSet rs1 = stmt.executeQuery("SELECT COUNT(*) FROM sys_menu WHERE id IN (12, 40, 42)");
            rs1.next();
            System.out.println("旧菜单残留: " + rs1.getInt(1) + " (应为0)");

            ResultSet rs2 = stmt.executeQuery("SELECT id, parent_id, menu_name, sort_no FROM sys_menu WHERE id = 41");
            rs2.next();
            System.out.println("投资者列表: id=" + rs2.getInt(1) + " parent_id=" + rs2.getInt(2) + " name=" + rs2.getString(3) + " sort=" + rs2.getInt(4));

            ResultSet rs3 = stmt.executeQuery("SELECT id, parent_id, menu_name, sort_no FROM sys_menu WHERE parent_id = 10 ORDER BY sort_no");
            System.out.println("门店管理子菜单:");
            while (rs3.next()) {
                System.out.println("  id=" + rs3.getInt(1) + " name=" + rs3.getString(3) + " sort=" + rs3.getInt(4));
            }

            ResultSet rs4 = stmt.executeQuery("SELECT COUNT(*) FROM investor_store WHERE investment_ratio > 0 AND investment_ratio < 1");
            rs4.next();
            System.out.println("小数投资比例残留: " + rs4.getInt(1) + " (应为0)");

            ResultSet rs5 = stmt.executeQuery("SELECT id, investment_ratio FROM investor_store WHERE investment_ratio > 0 LIMIT 5");
            System.out.println("投资比例样例:");
            while (rs5.next()) {
                System.out.println("  id=" + rs5.getInt(1) + " ratio=" + rs5.getDouble(2));
            }

            System.out.println("\n=== 迁移完成! ===");
        }
    }
}
