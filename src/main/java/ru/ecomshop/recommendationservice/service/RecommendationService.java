package ru.ecomshop.recommendationservice.service;

import lombok.AllArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.ecomshop.recommendationservice.model.dto.ProductResponse;
import ru.ecomshop.recommendationservice.model.dto.PurchaseItemResponse;
import ru.ecomshop.recommendationservice.model.dto.PurchaseResponse;
import ru.ecomshop.recommendationservice.model.dto.UserResponse;
import ru.ecomshop.recommendationservice.model.entity.NonPersonalRecommendation;
import ru.ecomshop.recommendationservice.model.entity.ParameterType;
import ru.ecomshop.recommendationservice.model.entity.ParameterValueType;
import ru.ecomshop.recommendationservice.model.entity.PersonalRecommendationParameter;
import ru.ecomshop.recommendationservice.repository.NonPersonalRecommendationRepository;
import ru.ecomshop.recommendationservice.repository.PersonalRecommendationParameterRepository;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RecommendationService {

    private final NonPersonalRecommendationRepository nonPersonalRecommendationRepository;

    private final PersonalRecommendationParameterRepository personalRecommendationParameterRepository;

    private final RestTemplate restTemplate;

    private static final String PRODUCT_SERVICE_URL = "http://localhost:3031/products/";
    private static final String USER_SERVICE_URL = "http://localhost:3030/users/";
    private static final String PURCHASE_SERVICE_URL = "http://localhost:3032/purchases/";
    // Параметр, отввечающий за то, сколько персоналаизированных рекоммендаций будет предложено
    private static final int MAX_PERSONAL_RECOMMENDATION_VALUES = 8;

    public List<Long> getRecommendations(Long userId) {
        // Пытаемся получить список покупок для пользователя
        URI uri = URI.create(PURCHASE_SERVICE_URL + "user/" + userId);
        ResponseEntity<List<PurchaseResponse>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        List<PurchaseResponse> userPurchases = response.getBody();

        if (userPurchases == null) {
            throw new RuntimeException("Произошла техническая ошибка");
        } else if (userPurchases.isEmpty()) {
            // Если пользователь еще не совершал ни одной покупки - вернем неперсонализированные рекомендации
            return getNonPersonalRecommendations();
        }
        // Если все ок, то возвращаем персонализированные рекомендации
        return getPersonalRecommendations(userId);
    }

    private List<Long> getNonPersonalRecommendations() {
        // Берем неперсонализированные рекомендации из БД
        return nonPersonalRecommendationRepository.findAll().stream()
                .map(NonPersonalRecommendation::getProductId).toList();
    }

    private List<Long> getPersonalRecommendations(Long userId) {
       List<Long> personalRecommendations = new ArrayList<>();

       // Получаем параметры персонализированных реокмендаций из БД
       List<PersonalRecommendationParameter> recommendationParameters = personalRecommendationParameterRepository.findAll();
        // Разделим на два списка по типу параметров (параметры пользователей и товаров)
        Map<Boolean, List<PersonalRecommendationParameter>> partitionedParameters = recommendationParameters.stream()
                .collect(Collectors.partitioningBy(param -> param.getParameterType() == ParameterType.USER));
        // Получаем два отдельных списка
        List<PersonalRecommendationParameter> userParameters = partitionedParameters.get(true);
        List<PersonalRecommendationParameter> productParameters = partitionedParameters.get(false);

        // Рекомендуемые товары на основе параметров пользователей
        List<Long> userRecommendations = getRecommendationsByUserParameters(userParameters, userId);
        // Рекомендуемые товары на основе параметров товаров
        List<Long> productRecommendations = getRecommendationsByProductParameters(productParameters);

        // Объединяем в один список
        personalRecommendations.addAll(userRecommendations);
        personalRecommendations.addAll(productRecommendations);

        // Получаем непермонализированные рекомендации
        List<Long> nonPersonalRecommendations = getNonPersonalRecommendations();
        // Список итоговых рекомендаций
        List<Long> finalRecommendations = new ArrayList<>(10);
        int count = 0;
        for (Long recommendation : personalRecommendations) {
            if (count < MAX_PERSONAL_RECOMMENDATION_VALUES) {
                finalRecommendations.add(recommendation);
                count++;
            } else {
                break;
            }
        }
        // Если итоговый список рекомендаций меньше 10 элементов, добавляем элементы из списка неперсонализированных рекомендаций
        int totalLimit = 10;

        count = finalRecommendations.size();
        for (Long recommendation : nonPersonalRecommendations) {
            if (count < totalLimit) {
                finalRecommendations.add(recommendation);
                count++;
            } else {
                break;
            }
        }

        // Ура, мы собрали персонализированные рекоммендации для пользоавтеля
        return finalRecommendations;
    }

    private List<Long> getRecommendationsByUserParameters(List<PersonalRecommendationParameter> userParameters,
                                                          Long userId) {
        URI uri = URI.create(USER_SERVICE_URL + userId);
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        UserResponse user = response.getBody();

        // Строим запрос в БД по переданным параметрам
        String query = buildUserQuery(userParameters, user);
        URI userUri = URI.create(USER_SERVICE_URL + "execute-query");
        // Получаем пользователей по заданным критериям
        List<Long> userIds = restTemplate.postForEntity(userUri, query, List.class).getBody();
        List<Long> usersPurchasesItems = new ArrayList<>();
        // Для каждого пользователя для каждой его покупки забираем идентификаторы товаров
        for (Long id : userIds) {
            URI currentUserUri = URI.create(PURCHASE_SERVICE_URL + "user/" + id);
            ResponseEntity<List<PurchaseResponse>> currentUserPurchases = restTemplate.exchange(
                    currentUserUri,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            List<PurchaseResponse> userPurchases = currentUserPurchases.getBody();
            for (PurchaseResponse purchaseResponse : userPurchases) {
                usersPurchasesItems.addAll(purchaseResponse.items().stream().map(PurchaseItemResponse::id).toList());
            }
        }
        return usersPurchasesItems;
    }

    private List<Long> getRecommendationsByProductParameters(List<PersonalRecommendationParameter> productParameters) {
        // Строим запрос в БД по переданным параметрам
        String query = buildProductQuery(productParameters);
        URI uri = URI.create(PRODUCT_SERVICE_URL + "execute-query");
        List<Long> productIds = restTemplate.postForEntity(uri, query, List.class).getBody();

        return productIds;
    }

    // Метод для построения строкового запроса для пользователей
    private String buildUserQuery(List<PersonalRecommendationParameter> parameters, UserResponse user) {
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM users");

        if (!parameters.isEmpty()) {
            // Собираем услловия запроса
            queryBuilder.append(" WHERE ");
            StringJoiner joiner = new StringJoiner(" AND ");

            for (PersonalRecommendationParameter param : parameters) {
                String condition = buildConditionForUser(param, user);
                joiner.add(condition);
            }

            queryBuilder.append(joiner.toString());
        }

        return queryBuilder.toString();
    }

    // Метод для построения строкового запроса для товаров
    private String buildProductQuery(List<PersonalRecommendationParameter> parameters) {
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM products");

        // Присоединяем необходимые таблицы
        queryBuilder.append(" LEFT JOIN inventory i ON p.id = i.product_id")
                .append(" LEFT JOIN prices pr ON p.id = pr.product_id")
                .append(" LEFT JOIN product_attributes pa ON p.id = pa.product_id")
                .append(" LEFT JOIN product_images pi ON p.id = pi.product_id");

        if (!parameters.isEmpty()) {
            // Собираем услловия запроса
            queryBuilder.append(" WHERE ");
            StringJoiner joiner = new StringJoiner(" AND ");

            for (PersonalRecommendationParameter param : parameters) {
                String condition = buildConditionForProduct(param);
                joiner.add(condition);
            }

            queryBuilder.append(joiner.toString());
        }

        return queryBuilder.toString();
    }

    // Составляем условие запроса для пользователя
    private String buildConditionForUser(PersonalRecommendationParameter param, UserResponse user) {
        String paramName = param.getParameterName();
        String paramValue = param.getParameterValue();
        ParameterValueType valueType = param.getParameterValueType();

        switch (valueType) {
            // Для определенного значения
            case SPECIFIC_VALUE:
                return String.format("%s = '%s'", paramName, paramValue);
            // Для диапазона
            case RANGE_VALUES: {
                String[] range = paramValue.substring(1, paramValue.length() - 1).split(",");
                return String.format("%s BETWEEN '%s' AND '%s'", paramName, range[0], range[1]);
            }
            // Для разброса
//            case SPREAD_VALUES: {
//                int spread = Integer.parseInt(paramValue);
//                int referenceValue = getReferenceFieldValue(param);
//                return String.format("%s BETWEEN %d AND %d", paramName, referenceValue - spread, referenceValue + spread);
//            }

            default:
                throw new IllegalArgumentException("Unknown ParameterValueType");
        }
    }

    // Составляем условие запроса для товара
    private String buildConditionForProduct(PersonalRecommendationParameter param) {
        String paramName = param.getParameterName();
        String paramValue = param.getParameterValue();
        ParameterValueType valueType = param.getParameterValueType();

        switch (valueType) {
            // Для определенного значения
            case SPECIFIC_VALUE:
                return String.format("%s = '%s'", paramName, paramValue);
            // Для диапазона
            case RANGE_VALUES: {
                String[] range = paramValue.substring(1, paramValue.length() - 1).split(",");
                return String.format("%s BETWEEN '%s' AND '%s'", paramName, range[0], range[1]);
            }
            // Для разброса
//            case SPREAD_VALUES: {
//                int spread = Integer.parseInt(paramValue);
//                String referenceValue = getProductFieldValue(product, paramName);
//                return String.format("%s BETWEEN %d AND %d", paramName, referenceValue - spread, referenceValue + spread);
//            }

            default:
                throw new IllegalArgumentException("Unknown ParameterValueType");
        }
    }

    // Получаем значение конкретного поля пользователя
    private String getUserFieldValue(UserResponse user, String parameterName) {
        return switch (parameterName) {
            case "birthdate" -> user.birthdate().toString();
            case "registrationDate" -> user.registrationDate().toString();
            default -> "";
        };
    }

    // Получаем значение конкретного поля товара
    private String getProductFieldValue(ProductResponse product, String parameterName) {
        return switch (parameterName) {
            case "categoryId" -> product.categoryId().toString();
            case "brandId" -> product.brandId().toString();
            default -> "";
        };
    }

}
