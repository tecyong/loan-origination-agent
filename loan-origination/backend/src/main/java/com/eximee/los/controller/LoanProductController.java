package com.eximee.los.controller;

import com.eximee.los.dto.LoanProductDto;
import com.eximee.los.service.LoanProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Loan Products", description = "Loan product catalog and parameter queries")
public class LoanProductController {

    private final LoanProductService productService;

    public LoanProductController(LoanProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Get list of all active loan products")
    public ResponseEntity<List<LoanProductDto>> listProducts() {
        return ResponseEntity.ok(productService.getAllActiveProducts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get details of a specific loan product")
    public ResponseEntity<LoanProductDto> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }
}
