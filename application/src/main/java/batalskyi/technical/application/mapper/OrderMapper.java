package batalskyi.technical.application.mapper;

import batalskyi.technical.application.dto.OrderResponseDTO;
import batalskyi.technical.application.entity.Client;
import batalskyi.technical.application.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public interface OrderMapper {

  @Mapping(target = "consumerId", source = "consumer", qualifiedByName = "mapClientToId")
  @Mapping(target = "supplierId", source = "supplier", qualifiedByName = "mapClientToId")
  OrderResponseDTO toOrderResponseDto(Order order);

  @Named("mapClientToId")
  static Long mapClientToId(Client client) {
    return client != null ? client.getId() : null;
  }
}
