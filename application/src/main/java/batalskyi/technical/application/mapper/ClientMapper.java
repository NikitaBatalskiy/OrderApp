package batalskyi.technical.application.mapper;

import batalskyi.technical.application.dto.ClientCreateOrUpdateDTO;
import batalskyi.technical.application.dto.ClientDTO;
import batalskyi.technical.application.entity.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public interface ClientMapper {

  ClientDTO toClientDTO(Client client);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "profit", ignore = true)
  @Mapping(target = "deactivatedAt", ignore = true)
  Client clientCreateToClient(ClientCreateOrUpdateDTO clientDTO);

}
