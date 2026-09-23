package ru.mirea.project.repository;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;

import java.util.List;

// Интерфейс репозитория для работы с сущностями
public interface Repository<T, ID> {
    T findById(ID id) throws EntityNotFoundException;
    List<T> findAll();
    T save(T entity) throws BusinessException;
    void update(T entity) throws BusinessException, EntityNotFoundException;
    void delete(ID id) throws EntityNotFoundException;
}