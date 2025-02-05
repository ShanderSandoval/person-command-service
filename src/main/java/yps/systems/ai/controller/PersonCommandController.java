package yps.systems.ai.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;
import yps.systems.ai.model.Person;
import yps.systems.ai.repository.IPersonNodeRepository;

import java.util.Optional;

@RestController
@RequestMapping("/command/personService")
public class PersonCommandController {

    private final IPersonNodeRepository personNodeRepository;
    private final KafkaTemplate<String, Person> kafkaTemplate;

    @Value("${env.kafka.topicEvent}")
    private String kafkaTopicEvent;

    @Autowired
    public PersonCommandController(IPersonNodeRepository personNodeRepository, KafkaTemplate<String, Person> kafkaTemplate) {
        this.personNodeRepository = personNodeRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping
    public ResponseEntity<String> savePersonNode(@RequestBody Person person) {
        Person savedPerson = personNodeRepository.save(person);
        Message<Person> message = MessageBuilder
                .withPayload(person)
                .setHeader(KafkaHeaders.TOPIC, kafkaTopicEvent)
                .setHeader("eventType", "CREATE_PERSON")
                .setHeader("source", "personService")
                .build();
        kafkaTemplate.send(message);
        return new ResponseEntity<>("Person saved with ID: " + savedPerson.getElementId(), HttpStatus.CREATED);
    }

    @DeleteMapping("/{elementId}")
    public ResponseEntity<String> deletePersonNode(@PathVariable String elementId) {
        Optional<Person> personNodeOptional = personNodeRepository.findById(elementId);
        if (personNodeOptional.isPresent()) {
            personNodeRepository.deleteById(elementId);
            Message<String> message = MessageBuilder
                    .withPayload(elementId)
                    .setHeader(KafkaHeaders.TOPIC, kafkaTopicEvent)
                    .setHeader("eventType", "DELETE_PERSON")
                    .setHeader("source", "personService")
                    .build();
            kafkaTemplate.send(message);
            return new ResponseEntity<>("Person deleted successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Person not founded", HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{elementId}")
    public ResponseEntity<String> updatePersonNode(@PathVariable String elementId, @RequestBody Person person) {
        Optional<Person> personNodeFounded = personNodeRepository.findById(elementId);
        if (personNodeFounded.isPresent()) {
            person.setElementId(personNodeFounded.get().getElementId());
            personNodeRepository.save(person);
            Message<Person> message = MessageBuilder
                    .withPayload(person)
                    .setHeader(KafkaHeaders.TOPIC, kafkaTopicEvent)
                    .setHeader("eventType", "UPDATE_PERSON")
                    .setHeader("source", "personService")
                    .build();
            kafkaTemplate.send(message);
            return new ResponseEntity<>("Person updated successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Person not founded", HttpStatus.NOT_FOUND);
        }
    }
}
