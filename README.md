# PojoDB

Simple file-based storage with Spring Data-like usage.

## Usage

### Basic usage

```
// create storage handle
final PojoDB storage = PojoDB.builder()
	.withPath("/Users/miroslavhruz/upis/IdeaProjects/bos/flat-file-storage")
	.build();

// create repository
final DomainRepository<User> userRepository = storage.newDomainRepository("generic.users", User.class);

// 
final User user = new User();
user.setUsername("john");
user.setPassword("doe");
userRepository.save(user);
```

### Transaction usage


```
//create domain repository
final DomainRepository<QueueItem> queueRepository = storage.newDomainRepository("vox.queue", QueueItem.class);

final QueueItem item = new QueueItem();
item.setInputs(Arrays.asList("1", "2", "3"));
item.setState("testing");
item.setZones(Arrays.asList("a", "b"));

queueRepository.save(item);


//start new transaction
final Tx tx = storage.newTx();

//modify existing user
item.setState("MIRA");
item.setId(UUID.randomUUID().toString());
queueRepository.save(item);

item.setState("MIRA2");
item.setId(UUID.randomUUID().toString());
queueRepository.save(item);

//create new user
User user2 = new User();
user2.setId(UUID.randomUUID().toString());
user2.setUsername("daksmdkmadkms");
userRepository.save(user2);

//delete user
userRepository.delete(user);

//commit transaction
try {
    tx.commitOrRollback();
} catch (Exception e) {
    e.printStackTrace();
}
```