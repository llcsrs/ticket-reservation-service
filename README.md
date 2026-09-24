# SafeTicket - Ticket Reservation Service

Este é o microserviço de reserva de ingressos (Ticket Reservation Service) do projeto **SafeTicket**. Ele é responsável por gerenciar a intenção de compra de assentos para eventos, garantindo que não ocorra *double-booking* (venda dupla do mesmo assento).

## Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 3** (Web, Data JPA, Data Redis, Validation)
- **PostgreSQL** (Banco de dados relacional para persistência das reservas)
- **Redis** (Gerenciamento de cache e *Distributed Locks*)
- **Lombok** (Redução de código repetitivo)
- **Testcontainers** (Testes de integração)

## Arquitetura e Funções Principais

### 1. Reserva de Assentos com Lock Distribuído
A principal função da aplicação é reservar um assento para um usuário em um determinado evento. Para evitar condições de corrida (quando múltiplos usuários tentam comprar o mesmo ingresso ao mesmo tempo), o sistema utiliza **Locks Distribuídos através do Redis**.

- O sistema realiza uma operação atômica de `SETNX` (Set if Not eXists) no Redis.
- A chave de bloqueio é única para cada assento de um evento (`seat:lock:{eventId}:{seatNumber}`).

### 2. TTL (Time-To-Live) e Expiração Automática
- Quando a reserva inicial é feita e o lock é adquirido, é definido um tempo de expiração de **10 minutos**.
- Isso significa que o assento fica "bloqueado" e garantido para o usuário durante esse período, para que ele possa concluir a etapa de pagamento no gateway.
- Se o pagamento não for finalizado em 10 minutos, a chave expira no Redis automaticamente, liberando o assento para novas tentativas de compra.

### 3. Persistência e Transação
- Após garantir o lock no Redis, o sistema salva a intenção de reserva no banco de dados **PostgreSQL**.
- A reserva é persistida com o status `PENDING` (Pendente), armazenando o `eventId`, `userId`, `seatNumber`, e o momento em que a reserva irá expirar (`expiresAt`).
- O serviço utiliza a anotação `@Transactional` para garantir que o salvamento no banco de dados seja feito de forma segura e atômica.

## Resumo do Fluxo de Reserva
1. A aplicação recebe um pedido de reserva (`ReservationRequest`).
2. Tenta-se criar um bloqueio no Redis para aquele assento específico.
3. Se a chave já existir no Redis, significa que outra pessoa já está comprando o assento, então o sistema retorna um erro (`SeatAlreadyReservedException`).
4. Se a chave não existir, o sistema a cria (obtendo sucesso no lock).
5. A reserva é salva no banco de dados PostgreSQL com status `PENDING`.
6. A API retorna os dados da reserva, informando ao usuário que o assento está garantido por 10 minutos para efetuar o pagamento.
