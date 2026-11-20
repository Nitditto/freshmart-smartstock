package com.freshmart.smartstock.config;

import com.freshmart.smartstock.domain.entity.Category;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.Supplier;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.repository.CategoryRepository;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.SupplierRepository;
import com.freshmart.smartstock.repository.UserRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dev/demo seed (enabled by profile `dev`, property app.seed.enabled=true). Idempotent: skips if data exists.
 * Target (spec): 1 ADMIN, 2 STAFF, 5 categories, 30 products, 3 suppliers, 10 documents.
 * Part 1 (this file): users, categories, products, suppliers.
 * TODO[Member A] Part 2 (after Member C merges): create ~10 receipts/issues by calling
 *   ReceiptService/IssueService so stock is produced by real confirmed documents, not by direct UPDATEs.
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppProperties props;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    public void run(String... args) {
        String password = props.seed().defaultPassword();
        if (password == null || password.isBlank()) {
            log.warn("Seed skipped: app.seed.default-password is empty");
            return;
        }
        if (userRepository.count() == 0) {
            seedUsers(password);
        }
        if (categoryRepository.count() == 0) {
            seedCatalog();
        }
        if (supplierRepository.count() == 0) {
            seedSuppliers();
        }
        log.info("Seed finished: users={}, categories={}, products={}, suppliers={}",
                userRepository.count(), categoryRepository.count(),
                productRepository.count(), supplierRepository.count());
    }

    private void seedUsers(String password) {
        userRepository.save(newUser("Admin FreshMart", "admin@freshmart.local", Role.ADMIN, password));
        userRepository.save(newUser("Nhân viên kho 1", "staff1@freshmart.local", Role.STAFF, password));
        userRepository.save(newUser("Nhân viên kho 2", "staff2@freshmart.local", Role.STAFF, password));
    }

    private User newUser(String fullName, String email, Role role, String rawPassword) {
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setRole(role);
        u.setActive(true);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        return u;
    }

    /** 5 categories x 6 products = 30 products. Each item = "name|unit|price|minStock". */
    private void seedCatalog() {
        String[][] catalog = {
                {"Rau củ quả", "RCQ",
                        "Cà chua|kg|28000|20", "Khoai tây|kg|22000|30", "Cà rốt|kg|20000|25",
                        "Bắp cải|kg|18000|20", "Chuối|nải|35000|10", "Táo Fuji|kg|65000|15"},
                {"Thịt & cá", "TCA",
                        "Thịt ba chỉ|kg|135000|15", "Ức gà|kg|85000|15", "Cá hồi phi lê|kg|320000|5",
                        "Tôm sú|kg|280000|5", "Thịt bò xay|kg|210000|10", "Cá basa phi lê|kg|65000|10"},
                {"Sữa & trứng", "STR",
                        "Sữa tươi 1L|hộp|32000|40", "Sữa chua|lốc|28000|30", "Trứng gà|vỉ|42000|25",
                        "Phô mai lát|gói|55000|10", "Bơ nhạt|thanh|48000|10", "Sữa đặc|lon|24000|20"},
                {"Đồ khô", "DKH",
                        "Gạo ST25 5kg|bao|140000|20", "Mì gói|thùng|115000|15", "Dầu ăn 1L|chai|52000|20",
                        "Đường trắng 1kg|gói|24000|20", "Nước mắm 500ml|chai|38000|15", "Muối i-ốt|gói|8000|30"},
                {"Đồ uống", "DUO",
                        "Nước suối 500ml|thùng|78000|20", "Nước ngọt lon|thùng|210000|10", "Trà xanh|chai|10000|50",
                        "Cà phê hòa tan|hộp|95000|10", "Bia lon|thùng|390000|8", "Nước cam ép|chai|25000|20"}
        };
        for (String[] group : catalog) {
            Category c = new Category();
            c.setName(group[0]);
            c = categoryRepository.save(c);
            for (int i = 2; i < group.length; i++) {
                String[] f = group[i].split("\\|");
                Product p = new Product();
                p.setSku("%s-%03d".formatted(group[1], i - 1));
                p.setName(f[0]);
                p.setCategory(c);
                p.setUnit(f[1]);
                p.setPrice(new BigDecimal(f[2]));
                p.setMinStock(Integer.parseInt(f[3]));
                p.setQuantityInStock(0);
                p.setActive(true);
                productRepository.save(p);
            }
        }
    }

    private void seedSuppliers() {
        supplierRepository.save(newSupplier("Công ty Nông sản Xanh", "0901000001", "Đà Lạt, Lâm Đồng"));
        supplierRepository.save(newSupplier("Công ty Thực phẩm Tươi Sống Việt", "0901000002", "Quận 7, TP.HCM"));
        supplierRepository.save(newSupplier("Đại lý Đồ uống Miền Nam", "0901000003", "Thủ Đức, TP.HCM"));
    }

    private Supplier newSupplier(String name, String phone, String address) {
        Supplier s = new Supplier();
        s.setName(name);
        s.setPhone(phone);
        s.setAddress(address);
        return s;
    }
}
