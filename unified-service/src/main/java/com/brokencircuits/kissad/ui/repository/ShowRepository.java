package com.brokencircuits.kissad.ui.repository;

import com.brokencircuits.kissad.ui.domain.ShowDto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowRepository extends JpaRepository<ShowDto, String> {

}