package com.eximee.los.config;

import com.eximee.los.domain.LoanProduct;
import com.eximee.los.domain.Role;
import com.eximee.los.domain.User;
import com.eximee.los.repository.LoanProductRepository;
import com.eximee.los.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final LoanProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            LoanProductRepository productRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedProducts();
    }

    private void seedUsers() {
        if (!userRepository.existsByEmail("applicant@demo.com")) {
            userRepository.save(new User(
                    "applicant@demo.com",
                    passwordEncoder.encode("password123"),
                    "Alex Morgan",
                    "+1-555-0199",
                    Role.ROLE_APPLICANT
            ));
            log.info("Seeded demo applicant: applicant@demo.com / password123");
        }

        if (!userRepository.existsByEmail("officer@demo.com")) {
            userRepository.save(new User(
                    "officer@demo.com",
                    passwordEncoder.encode("password123"),
                    "Sarah Jenkins (Senior Underwriter)",
                    "+1-555-0144",
                    Role.ROLE_OFFICER
            ));
            log.info("Seeded demo officer: officer@demo.com / password123");
        }

        if (!userRepository.existsByEmail("admin@demo.com")) {
            userRepository.save(new User(
                    "admin@demo.com",
                    passwordEncoder.encode("password123"),
                    "System Administrator",
                    "+1-555-0100",
                    Role.ROLE_ADMIN
            ));
            log.info("Seeded demo admin: admin@demo.com / password123");
        }
    }

    private void seedProducts() {
        if (productRepository.count() == 0) {
            LoanProduct p1 = new LoanProduct();
            p1.setCode("PERSONAL_FLEXI");
            p1.setName("Personal Flexi Loan");
            p1.setDescription("Unsecured personal loan for flexible individual financing needs with fixed monthly installments.");
            p1.setMinAmount(BigDecimal.valueOf(1000.00));
            p1.setMaxAmount(BigDecimal.valueOf(50000.00));
            p1.setMinTenureMonths(6);
            p1.setMaxTenureMonths(60);
            p1.setInterestRate(BigDecimal.valueOf(8.50));
            p1.setIsActive(true);
            productRepository.save(p1);

            LoanProduct p2 = new LoanProduct();
            p2.setCode("AUTO_DIRECT");
            p2.setName("Direct Auto Finance");
            p2.setDescription("Competitive auto financing for new and pre-owned vehicles with rapid pre-qualification.");
            p2.setMinAmount(BigDecimal.valueOf(5000.00));
            p2.setMaxAmount(BigDecimal.valueOf(100000.00));
            p2.setMinTenureMonths(12);
            p2.setMaxTenureMonths(84);
            p2.setInterestRate(BigDecimal.valueOf(5.90));
            p2.setIsActive(true);
            productRepository.save(p2);

            LoanProduct p3 = new LoanProduct();
            p3.setCode("HOME_EQUITY");
            p3.setName("Home Prime Mortgage");
            p3.setDescription("Fixed-rate residential mortgage and home equity financing with favorable extended tenures.");
            p3.setMinAmount(BigDecimal.valueOf(25000.00));
            p3.setMaxAmount(BigDecimal.valueOf(1000000.00));
            p3.setMinTenureMonths(60);
            p3.setMaxTenureMonths(360);
            p3.setInterestRate(BigDecimal.valueOf(4.25));
            p3.setIsActive(true);
            productRepository.save(p3);

            log.info("Seeded 3 default loan products into database.");
        }
    }
}
