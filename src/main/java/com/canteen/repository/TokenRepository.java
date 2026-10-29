package com.canteen.repository;

import com.canteen.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    @Query("select max(t.tokenNumber) from Token t where t.tokenDate = :d")
    Integer maxTokenNumber(@Param("d") LocalDate d);

    Optional<Token> findByTokenDateAndTokenNumber(LocalDate date, int tokenNumber);
}
