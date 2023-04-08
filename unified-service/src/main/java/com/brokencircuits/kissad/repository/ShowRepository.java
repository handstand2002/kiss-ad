package com.brokencircuits.kissad.repository;

import com.brokencircuits.kissad.domain.ShowDto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowRepository extends JpaRepository<ShowDto, String> {

}