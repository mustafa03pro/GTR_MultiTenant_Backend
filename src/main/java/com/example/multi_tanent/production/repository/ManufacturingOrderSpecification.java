package com.example.multi_tanent.production.repository;

import com.example.multi_tanent.production.entity.ManufacturingOrder;
import com.example.multi_tanent.production.enums.ManufacturingOrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class ManufacturingOrderSpecification {

    public static Specification<ManufacturingOrder> getSpecifications(Long tenantId, String search,
            ManufacturingOrderStatus status,
            LocalDate fromDate, LocalDate toDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Tenant filter
            predicates.add(criteriaBuilder.equal(root.get("tenant").get("id"), tenantId));

            // Search filter (MO Number, Item Code, Item Name)
            if (StringUtils.hasText(search)) {
                String searchLike = "%" + search.toLowerCase() + "%";
                Predicate moNumberPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("moNumber")),
                        searchLike);
                Predicate itemCodePredicate = criteriaBuilder
                        .like(criteriaBuilder.lower(root.get("item").get("itemCode")), searchLike);
                Predicate itemNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("item").get("name")),
                        searchLike);

                predicates.add(criteriaBuilder.or(moNumberPredicate, itemCodePredicate, itemNamePredicate));
            }

            // Status filter
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // Date Range filter (createdAt)
            if (fromDate != null) {
                OffsetDateTime startOfDay = fromDate.atStartOfDay().atOffset(ZoneOffset.UTC); // Adjust timezone if
                                                                                              // needed (e.g., system
                                                                                              // default)
                // Assuming stored in UTC or handling offset correctly. ideally user passes
                // offset date time but local date simplifies UI.
                // Converting LocalDate to OffsetDateTime at start of day UTC for simplicity or
                // use Tenant Context timezone.
                // For now using UTC.
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startOfDay));
            }

            if (toDate != null) {
                OffsetDateTime endOfDay = toDate.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endOfDay));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
