package com.canteen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "tokens", uniqueConstraints = @UniqueConstraint(columnNames = {"token_date", "token_number"}))
@Getter @Setter @NoArgsConstructor
public class Token {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "order_id")
    private CanteenOrder order;
    @Column(name = "token_number", nullable = false)
    private int tokenNumber;
    @Column(name = "token_date", nullable = false)
    private LocalDate tokenDate;

    public String getCode() {
        return String.format("T-%03d", tokenNumber);
    }
}
