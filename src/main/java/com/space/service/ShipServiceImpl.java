package com.space.service;

import com.space.BadRequestException;
import com.space.ShipNotFoundException;
import com.space.model.Ship;
import com.space.model.ShipType;
import com.space.repository.ShipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ShipServiceImpl implements ShipService {

    private ShipRepository shipRepository;

    @Autowired
    public void setShipRepository(ShipRepository shipRepository) {
        this.shipRepository = shipRepository;
    }

    @Override
    public List<Ship> getAllShips(Specification<Ship> specification) {
        return shipRepository.findAll(specification);
    }

    @Override
    public Ship createShip(Ship ship) {

        if (ship.getName() == null
                || ship.getShipType() == null
                || ship.getProdDate() == null
                || ship.getSpeed() == null
                || ship.getCrewSize() == null) {
            throw new BadRequestException("Ship parameters is not full");
        }

        checkShipParameters(ship);

        if (ship.getUsed() == null) {
            ship.setUsed(false);
        }

        Double rating = calculateRating(ship);

        ship.setRating(rating);

        return shipRepository.saveAndFlush(ship);
    }

    @Override
    public Ship editShip(Long id, Ship ship) {

        checkShipParameters(ship);

        if (!shipRepository.existsById(id)) {
            throw new ShipNotFoundException("Ship not found");
        }

        Ship shipToEdit = shipRepository.findById(id).get();

        if (ship.getName() != null) {
            shipToEdit.setName(ship.getName());
        }
        if (ship.getPlanet() != null) {
            shipToEdit.setPlanet(ship.getPlanet());
        }
        if (ship.getShipType() != null) {
            shipToEdit.setShipType(ship.getShipType());
        }
        if (ship.getProdDate() != null) {
            shipToEdit.setProdDate(ship.getProdDate());
        }
        if (ship.getUsed() != null) {
            shipToEdit.setUsed(ship.getUsed());
        }
        if (ship.getSpeed() != null) {
            shipToEdit.setSpeed(ship.getSpeed());
        }
        if (ship.getCrewSize() != null) {
            shipToEdit.setCrewSize(ship.getCrewSize());
        }
        Double rating = calculateRating(shipToEdit);
        shipToEdit.setRating(rating);

        return shipRepository.save(shipToEdit);
    }

    @Override
    public void deleteShip(Long id) {

        if (shipRepository.existsById(id)) {
            shipRepository.deleteById(id);
        } else {
            throw new ShipNotFoundException("Ship not found");
        }
    }

    @Override
    public Ship getShip(Long id) {

        if (!shipRepository.existsById(id)) {
            throw new ShipNotFoundException("Ship not found");
        }
        return shipRepository.findById(id).get();
    }

    @Override
    public Specification<Ship> filterByName(String name) {
        return ((root, query, criteriaBuilder) ->
                name == null ? null
                        : criteriaBuilder.like(root.get("planet"), "%" + name + "%"));
    }

    @Override
    public Specification<Ship> filterByPlanet(String planet) {
        return ((root, query, criteriaBuilder) ->
                planet == null ? null
                        : criteriaBuilder.like(root.get("planet"), "%" + planet + "%"));
    }

    @Override
    public Specification<Ship> filterByShipType(ShipType shipType) {
        return ((root, query, criteriaBuilder) ->
                shipType == null ? null
                        : criteriaBuilder.equal(root.get("shipType"), shipType));
    }

    @Override
    public Specification<Ship> filterByDate(Long minDate, Long maxDate) {
        return ((root, query, criteriaBuilder) -> {
            if (minDate == null && maxDate == null) {
                return null;
            }
            if (minDate == null) {
                Date start = new Date(maxDate);
                return criteriaBuilder.lessThanOrEqualTo(root.get("prodDate"), start);
            }
            if (maxDate == null) {
                Date start = new Date(minDate);
                return criteriaBuilder.greaterThanOrEqualTo(root.get("prodDate"), start);
            }

            Date start = new Date(minDate);
            Date end = new Date(maxDate);
            return criteriaBuilder.between(root.get("prodDate"), start, end);
        });
    }

    @Override
    public Specification<Ship> filterByUsage(Boolean isUsed) {
        return ((root, query, criteriaBuilder) -> {
            if (isUsed == null) {
                return null;
            }
            return isUsed ? criteriaBuilder.isTrue(root.get("isUsed"))
                    : criteriaBuilder.isFalse(root.get("isUsed"));
        });
    }

    @Override
    public Specification<Ship> filterBySpeed(Double minSpeed, Double maxSpeed) {
        return ((root, query, criteriaBuilder) -> {
            if (minSpeed == null && maxSpeed == null) {
                return null;
            }
            if (minSpeed == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("speed"), maxSpeed);
            }
            if (maxSpeed == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("speed"), minSpeed);
            }

            return criteriaBuilder.between(root.get("speed"), minSpeed, maxSpeed);
        });
    }

    @Override
    public Specification<Ship> filterByCrewSize(Integer min, Integer max) {
        return ((root, query, criteriaBuilder) -> {
            if (min == null && max == null) {
                return null;
            }

            if (min == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("crewSize"), max);
            }
            if (max == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("crewSize"), min);
            }

            return criteriaBuilder.between(root.get("crewSize"), min, max);
        });
    }

    @Override
    public Specification<Ship> filterByRating(Double min, Double max) {
        return ((root, query, criteriaBuilder) -> {
            if (min == null && max == null) {
                return null;
            }
            if (min == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("rating"), max);
            }
            if (max == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), min);
            }
            return criteriaBuilder.between(root.get("rating"), min, max);
        });
    }

    @Override
    public Page<Ship> getAllShips(Specification<Ship> shipSpecification, Pageable sortedByName) {
        return shipRepository.findAll(shipSpecification, sortedByName);
    }

    private void checkShipParameters(Ship ship) {

        if (ship.getName() != null
                && (ship.getName().length() < 1 || ship.getName().length() > 50)) {
            throw new BadRequestException("Incorrect ship name");
        }

        if (ship.getPlanet() != null
                && (ship.getPlanet().length() < 1 || ship.getPlanet().length() > 50)) {
            throw new BadRequestException("Incorrect planet name");
        }

        if (ship.getCrewSize() != null
                && (ship.getCrewSize() < 1 || ship.getCrewSize() > 50)) {
            throw new BadRequestException("Incorrect crew size");
        }

        if (ship.getSpeed() != null
                && (ship.getSpeed() < 0.01D || ship.getSpeed() > 0.99D)) {
            throw new BadRequestException("Incorrect ship speed");
        }

        if (ship.getProdDate() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(ship.getProdDate());

            if (calendar.get(Calendar.YEAR) < 2800
                    || calendar.get(Calendar.YEAR) > 3019) {
                throw new BadRequestException("Incorrect ship production year");
            }
        }
    }

    private Double calculateRating(Ship ship) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(ship.getProdDate());

        Double v = ship.getSpeed();
        double k = ship.getUsed() ? 0.5 : 1;
        int y0 = 3019;
        int y1 = calendar.get(Calendar.YEAR);

        // formula
        double calculation = (80 * v * k) / y0 - y1 + 1;

        BigDecimal rating = new BigDecimal(calculation);

        return rating.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
