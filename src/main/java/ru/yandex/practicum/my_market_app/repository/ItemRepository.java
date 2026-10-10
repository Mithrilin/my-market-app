package ru.yandex.practicum.my_market_app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.my_market_app.model.Item;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String searchTitle,
                                                                                String searchDescription,
                                                                                Pageable pageable);

    List<Item> findByCountGreaterThanEqual(Integer minCountItemsInCart);
}
