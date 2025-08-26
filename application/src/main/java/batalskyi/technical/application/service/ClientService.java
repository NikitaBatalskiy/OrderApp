package batalskyi.technical.application.service;

import batalskyi.technical.application.dto.ClientCreateOrUpdateDTO;
import batalskyi.technical.application.dto.ClientDTO;
import batalskyi.technical.application.entity.Client;
import batalskyi.technical.application.exception.AttributeMismatchException;
import batalskyi.technical.application.exception.ClientNotFoundException;
import batalskyi.technical.application.exception.DuplicateEmailException;
import batalskyi.technical.application.mapper.ClientMapper;
import batalskyi.technical.application.repository.ClientRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class ClientService {

  private static final Set<String> FIELDS = Set.of("name", "email", "address");

  private final ClientRepository clientRepository;
  private final ClientMapper clientMapper;

  @Transactional(readOnly = true)
  public List<ClientDTO> getAllClients() {
    return clientRepository.findAll().stream().map(clientMapper::toClientDTO).toList();
  }

  public ClientDTO createClient(ClientCreateOrUpdateDTO clientDTO) {
    if (isEmailDuplicated(clientDTO.getEmail())) {
      throw new DuplicateEmailException("Email already exists: " + clientDTO.getEmail());
    }
    var client = clientRepository.save(clientMapper.clientCreateToClient(clientDTO));
    return clientMapper.toClientDTO(client);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ClientDTO updateClient(Long id, String name, String email, String address,
      Boolean active) {
    log.info("Updating client with id: {}.", id);
    if (name == null && email == null && address == null && active == null) {
      log.error("At least one attribute should be provided for update.");
      throw new AttributeMismatchException("At least one attribute should be provided for update");
    }

    var client = getClientById(id);

    if (name != null) {
      log.info("New client name: {}.", name);
      client.setName(name);
    }
    if (email != null) {
      if (isEmailDuplicated(email) && !email.equals(client.getEmail())) {
        log.error("Email already exists: {}.", email);
        throw new DuplicateEmailException("Email already exists: " + email);
      }
      log.info("New client email: {}.", email);
      client.setEmail(email);
    }
    if (address != null) {
      log.info("New client address: {}.", address);
      client.setAddress(address);
    }
    if (active != null) {
      log.info("New client active: {}.", active);
      client.setActive(active);
      client.setDeactivatedAt(active ? null : LocalDateTime.now());
    }

    log.info("Update finished.");
    var saved = clientRepository.save(client);
    return clientMapper.toClientDTO(saved);
  }

  private boolean isEmailDuplicated(String email) {
    return clientRepository.findByEmail(email).isPresent();
  }

  public List<ClientDTO> searchClients(String field, String text) {
    if (!FIELDS.contains(field.toLowerCase())) {
      log.error("Search client with field '{}' is not supported.", field);
      throw new AttributeMismatchException(
          "Search client with field '" + field + "' is not supported.");
    }
    if (text == null || text.length() < 3) {
      log.error("Search client with text length less than 3 is not supported.");
      throw new AttributeMismatchException(
          "Search client with text length less than 3 is not supported.");
    }
    log.info("Search client with '{}' that contains '{}'", field, text);
    return clientRepository.findAll((root, cq, cb) ->
            cb.like(cb.lower(root.get(field)), "%" + text.toLowerCase() + "%"))
        .stream()
        .map(clientMapper::toClientDTO)
        .toList();
  }

  public Client getClientById(Long id) {
    log.info("Searching client with id: {}.", id);
    var client = clientRepository.findById(id);
    if (client.isEmpty()) {
      log.error("Client with id {} not found.", id);
      throw new ClientNotFoundException("Client with id " + id + " not found.");
    }
    return client.get();
  }

  public BigDecimal getProfitById(Long id) {
    var client = getClientById(id);
    return client.getProfit();
  }

  public List<ClientDTO> searchClientsInProfitRange(BigDecimal min, BigDecimal max) {
    if (max.compareTo(min) < 0) {
      log.error("Attribute 'max' should be greater than 'min'.");
      throw new AttributeMismatchException("Attribute 'max' should be greater than 'min'.");
    }
    log.info("Collecting list of clients with profit range between {} and {}.", min, max);
    return clientRepository.findAll((root, cq, cb) ->
            cb.between(root.get("profit"), min, max))
        .stream()
        .map(clientMapper::toClientDTO)
        .toList();
  }
}
