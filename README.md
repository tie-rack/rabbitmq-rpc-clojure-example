# RabbitMQ-backed RPC for a Pedestal Service

## Running it all

Install [RabbitMQ](https://www.rabbitmq.com/) and run it with the
default configuration.

In each of the `fact`, `fibs`, and `web` directories, call `lein
run`. Then hit (for example)
[localhost:8080/number-info/13](http://localhost:8080/number-info/13)

Run as many `fact` and `fibs` workers as you like. They'll share the
load.

The only shared configuration between callers and callees is the
RabbitMQ connection info and the queue names. But that's pretty much
exactly what you'd expect.

## Caveats

There's currently a memory leak in here. The `pending-rpc-calls` map
for each RPC service is never cleaned of unhandled messages. Purging
those wouldn't be too difficult to put in place.

The `rpc/service` function should probably be a macro,
`defrpc-service`, which would `defn` the RPC call fn (instead of
returning it) and maybe `def` the `pending-rpc-calls` and
`response-queue-name` (to allow you to get a handle on them in a REPL
instead of closing over them in that `let`).

And, of course, the `web.rpc` namespace should be it's own library.
