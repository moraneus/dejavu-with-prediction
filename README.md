```     
██╗██████╗ ██████╗ ██╗   ██╗      ██████╗ ███████╗     ██╗ █████╗ ██╗   ██╗██╗   ██╗
╚═╝██╔══██╗██╔══██╗██║   ██║      ██╔══██╗██╔════╝     ██║██╔══██╗██║   ██║██║   ██║
██║██████╔╝██████╔╝██║   ██║█████╗██║  ██║█████╗       ██║███████║██║   ██║██║   ██║
██║██╔═══╝ ██╔══██╗╚██╗ ██╔╝╚════╝██║  ██║██╔══╝  ██   ██║██╔══██║╚██╗ ██╔╝██║   ██║
██║██║     ██║  ██║ ╚████╔╝       ██████╔╝███████╗╚█████╔╝██║  ██║ ╚████╔╝ ╚██████╔╝
╚═╝╚═╝     ╚═╝  ╚═╝  ╚═══╝        ╚═════╝ ╚══════╝ ╚════╝ ╚═╝  ╚═╝  ╚═══╝   ╚═════╝ 
        
First-order past time LTL with recursive rules, time and prediction!
Version 3.0, Februar - 2023
```

  
## Overview

[DejaVu](https://github.com/havelund/dejavu) is a program written in Scala for monitoring event streams (traces) against temporal logic formulas.
The main formulas are written in a first-order past-time linear temporal logic, with the addition of macros and recursive rules. The logic also supports reasoning about time.
DejaVu contributors are [Klaus Havelund](http://www.havelund.com), [Doron Peled](http://u.cs.biu.ac.il/~doronp) and [Dogan Ulus](https://www.linkedin.com/in/doganulus).
iPRV DejaVu is an extension of DejaVu that supports prediction of the next k events.
More details about DejaVu and how it operates can be found [here](https://github.com/havelund/dejavu).

An example of a property in its most basic form is the following:

    prop closeOnlyOpenFiles : forall f . close(f) -> exists m . @ [open(f,m),close(f))

The property has the name `closeOnlyOpenFiles` and states that for any file `f`, if a `close(f)` event is observed, then there exists a mode `m` (e.g 'read' or 'write') such that in the previous step (`@`), some time in the past was observed an `open(f,m)` event, and since then no `close(f)` event has been observed. 

The implementation uses BDDs (Binary Decision Diagrams) for representing assignments to quantified variables (such as `f` and `m` above).

In this version of iPRV DejaVu we add the ability to predict the next `k` steps in 2 modes of prediction.

## System Requirements
Before installing DejaVu, ensure that you have the following software installed on your system:
* **Java 8**: DejaVu is implemented in Scala, which requires Java. Specifically, Java 8 is required. [Java 8 Installation Instructions](https://www.oracle.com/java/technologies/downloads/#java8)

## Installing DejaVu:

The directory [``dir``](https://github.com/moraneus/iPRV-DejaVu/tree/master/dir) contains files useful for installing and running DejaVu:
* [`dejavu`](https://github.com/moraneus/iPRV-DejaVu/blob/master/dir/dejavu)                          : Script to run the system
* [`dejavu.jar`](https://github.com/moraneus/iPRV-DejaVu/blob/master/dir/dejavu.jar)                  : Contain the dejavu.jar file

The directory [``out``](https://github.com/moraneus/iPRV-DejaVu/tree/master/out) contains useful examples for DejaVu:
* [``examples``](https://github.com/moraneus/iPRV-DejaVu/tree/master/out/examples)                        : An example directory containing properties and logs (DejaVu + iPRV-DejaVu)
* [``papers``](https://github.com/moraneus/iPRV-DejaVu/tree/master/out/papers)                            : A directory containing papers published about DejaVu and iPRV-DejaVu.

DejaVu is implemented in Scala. In this version of iPRV-DejaVu we used [**Scala 2.11.12**](https://www.scala-lang.org/download/2.11.12.html).

1. Install the Scala programming language if not already installed ([**Scala 2.11.12** installation instructions](https://www.scala-lang.org/download/2.11.12.html)).
2. Place the files ``dejavu`` and ``dejavu.jar`` mentioned above in some directory **DIR** (standing for the total path to this directory).
3. cd to  **DIR** and make the script executable:

        chmod +x dejavu

4. Preferably define an alias in your shell profile to the dejavu script so it can be called from anywhere:

        alias dejavu=DIR/dejavu

        
## Running DejaVu

The script is applied as follows:

    Usage:
    dejavu --specfile=<filename> --logfile=<filename> [OPTIONS]

    Options:
    -s, --specfile=<filename>           Path to the file containing the specification document. (Mandatory)
    -l, --logfile=<filename>            Path to the CSV log file to be analyzed. (Mandatory)
    -b, --bits=<numOfBits>              Number of bits for each variable in the BDD representation. (Default: 20 bits)
    -m, --mode=(debug|profile)          Set the output mode. (Default: None)
    -p, --prediction=<num>              Enable prediction and set the size of the prediction parameters. (Optional)
    -t, --prediction_type=(smart|brute) Specifies the prediction approach. (Default: smart)
    -e, --expected_verdict=(0|1)        Set the expected prediction verdict. If not specified, all possibilities will be predicted. (Optional)
    -c, --clear=(0|1)                   Clear generated files and folders. Set to '1' for cleaning. (Optional)
    
    Examples:
    dejavu --specfile=spec.txt --logfile=log.csv
    dejavu --specfile=spec.txt --logfile=log.csv --bits=16 --mode=debug --prediction=5 --prediction_type=brute --expected_verdict=1 --clear=1


#### Some Execution Examples

* Execute `iPRV-DejaVu`, allocating `10` bits to each variable.
  ```bash
  ./dejavu --specfile=/path/to/specfile --logfile=/path/to/logfile --bits=10
   ```
* Execute `iPRV-DejaVu`, allocating `7` bits to each variable, and ensure the clearing of generated files and folders.
  ```bash
  ./dejavu -s=/path/to/specfile --logfile=/path/to/logfile -b=7 --clear=1
  ```

* Execute `iPRV-DejaVu` in `debug` mode, where certain parameters like `bits` assume default values.
  ```bash
  ./dejavu --specfile=/path/to/specfile -l=/path/to/logfile --mode=debug
  ```

* Initiate the `iPRV-DejaVu` process, allocating `5` bits to each variable. Set the prediction parameter, `k`, to `3` 
  and employ a `brute-force` approach for the prediction type. The anticipated outcome is `false`, 
  indicating that the execution will cease immediately upon encountering a false prediction.
  ```bash
  ./dejavu --specfile=/path/to/specfile --logfile=/path/to/logfile --bits=5 --prediction=3 --prediction_type=brute -expected_verdict=0
  ```
* Initiate the `iPRV-DejaVu` process, allocating `5` bits to each variable. Set the prediction parameter, `k`, to `3`
  and employ a `smart` approach for the prediction type. The anticipated outcome is `true`,
  indicating that the execution will cease immediately upon encountering a true prediction.
  ```bash
  ./dejavu -s=/path/to/specfile -l=/path/to/logfile -b=5 -p=3 -t=smart -e=1
  ```

The `--specfile=<filename>` is the temporal specification that the trace must satisfy, 
which is referred to as the **Specification Document File**. See the explanation of the specification language below.

**The log file** (``--logfile=<filename>``) should be in comma separated value format (CSV): http://edoceo.com/utilitas/csv-file-format. For example, a file of
the form:

    list,chair,500
    bid,chair,700
    bid,chair,650
    sell,chair

with **no leading spaces** would mean the four events:

    list(chair,500)
    bid(chair,700)
    bid(chair,650)
    sell(chair)

**The log file and time**

In case the log file contains time stamps, the log file **name**! must contain the text `.timed.`. E.g: `log42.timed.csv`. Time stamps (natural numbers) must appear as the last argument to all events. E.g. a timed version of the above trace (with time values in
the range 1000 ... 1099) is:

    list,chair,500,1010
    bid,chair,700,1025
    bid,chair,650,1067
    sell,chair,1099 

Note that this last time value is not refered to explicitly in events in specifications.
That is, the events with time in the above CSV format still corresponds to the following
events in specification format:

    list(chair,500)
    bid(chair,700)
    bid(chair,650)
    sell(chair)

In case the log file is not timed (as described just above), time is considered as always being 0.
One can still check timed properties against such a log, but of course it makes little sense.

**The bits per variable** (``--bits=numOfBits``):
Indicates how many bits are assigned to each variable in the BDDs. 
This parameter is optional with the default value being 20. If the number is too low an error message will be issued 
during analysis as explained below. A too high number can have impact on the efficiency of the algorithm. Note that the 
number of values representable by N bits is 2^N, so one in general does not need very large numbers.  

The algorithm/implementation will perform garbage collection on allocated
BDDs, re-using BDDs that are no longer needed for checking the property, depending on the form of the formula.

**Debugging** (`--mode=debug`): 
Typically, a low number of bits (e.g., `3`) is chosen for 
debugging purposes. The result is a debugging output that displays the progress of formula 
evaluation for each event and the progress of the prediction, if activated. The output includes 
BDD graphs that can be visualized with GraphViz (http://www.graphviz.org).

**Prediction Parameter** (`--prediction=n`):
This flag determines whether to predict optional events of size 'n'. 
Additional supporting flags include `--prediction_type`, which outlines the prediction methodology, 
and `--expected_verdict`, which sets a target verdict for the evaluation process.

**Cleanup of Results and Created Files** (`--clear=1`): 
This flag indicates whether the files and folders generated during the process should 
be deleted. A value of `1` signifies deletion of the files, while the default value, `0`, 
implies that the files will be preserved in the `output` folder. The generated files typically 
include `TraceMonitor.scala`, `ast.dot`, and `dejavu-results`.


## Results from DejaVu

**Wellformedness errors** 

Error messages will be emitted if the specification document is not wellformed. A wellformedness
violation terminates the program, and can be one of the following:

* *Syntax error*: the document does not follow the grammar.
* *Free variable*: a used variable has not been introduced as a predicate parameter or a quantified variable.
* *Hiding*: a quantified expression hides a name introduced at an outer level.
* *Unused variable*: a name introduced as a predicate parameter or quantifier is not used.
* *Inconsistent*: a predicate is called with inconsistent number of arguments compared to definition or other calls.
* *Duplicates*: duplicate definition of a predicate or property name.
* *Undefined event*: events have been defined but an event is used and not amongst the defined ones.
* *Variable duplication*: a variable is introduced more than once in an event, macro, or rule parameter list.
* *Unprotected recursive rule definition*: A rule is called in the body of a rule definition wthout being underneath a previous-time operator: `@`.

A warning does not terminate the program, and can be one of:

* *Unused macro*: a predicate macro is defined but not used.
* *Unused event*: an event is defined but not used.
    
**Property violations** 
The tool will indicate a violation of a property by printing
what event number it concerns and what event. For example:

    *** Property incr violated on event number 3:

    #########################################################
    #### bid(chair,650)
    #########################################################  
    
indicates that event number 3 violates the property `incr`, and that event is a line in the CSV file having the format:

    bid,chair,650   

**Trace statistics**
A trace statistics is printed, which indicates how many events were processed in total and how they were distributed over the different event types:
  
    Processed 1100006 events

    ==================
    Event Counts:
    ------------------
    logout : 20001
    open   : 520001
    login  : 500000
    close  : 40002
    access : 20002
    ==================

Warnings will also be here be issued if: 

* if there are events in the specification that do not occur in the trace, or dually
* if there are events in the trace that do not occur in the specification

Both can potentially be signs of a flawed specification, but might not need to be.

**Not enough bits per variable**
If not enough bits have been allocated for a variable to hold the number of values generated for that variable, an assertion violation like the following is printed:

    *** java.lang.AssertionError: assertion failed: 
        10 bits is not enough to represent variable i.

One can/should experiment with BDD sizes.

**Prediction Outcomes**
The tool will actively present new predictions in the console, each accompanied by a 
succinct summary to facilitate understanding.

    ######### SUMMARY OF PREDICTION #########

        b(1)=1 -> g(2)=1 -> v(8)=0 -> DONE
        Processed 3 events
        
        3 errors detected!
        
        ==================
        Event Counts:
        ------------------
        g : 12
        t : 13
        v : 2
        b : 7
        ==================

    - Garbage collector was not activated

In the above example the DejaVu's prediction result indicates that there are k=3 events, as denoted by "Processed 3 events." 
Among these, the first two events satisfy the specification criteria 
(both equal to 1, which signifies True), whereas the third event violate the specification 
(equal to 0, representing False). 
The remaining output information presents a comprehensive evaluation, 
including statistics and the latest predicted events.

**Timing results**
The system will print the following timings:

* the time spent on parsing spec and synthesizing the monitor (a Scala program)
* the time spent on compiling the synthesized monitor
* the time spent on verifying trace with compiled monitor

**Generated files**
The system will generate the following files (none of which need any attention from the user, but may be informative): 

* TraceMonitor.scala : containing the synthesized monitor (a self-sufficient Scala program).
* ast.dot : file showing the structure of the formula (used for generating BDD    updating code). This can be viewed with GraphViz (http://www.graphviz.org). These two files help illustrate how the algorithm works.
* dejavu-results : contains numbers of events that violated property if any violations occurred. Mostly used for unit testing purposes.

## The DejaVu Specification Logic

### Grammar

The grammar for the DejaVu temporal logic is as follows.

    <doc> ::= <def> ... <def>
    <def> ::= <eventdef> | <macrodef> | <propertydef>

    <eventdef> ::= 'pred' <event>,...,<event>
    <event>    ::= <id> [ '(' <id> ',' ... ',' <id> ')' ]

    <macrodef> ::= 'pred' <id> [ '(' <id> ',' ... ',' <id> ')' ] '=' <form>

    <propertydef> ::= 'prop' <id> ':' <form> ['where' <ruledef> ',' ... ',' <ruledef>]

    <ruledef> ::= <id> ['(' <id> ',' ... ',' <id> ')'] ':=' <form>

    <form> ::= 
         'true' 
       | 'false' 
       | <id> [ '(' <param> ',' ... ',' <param> `)' ]
       | <form> <binop> <form> 
       | '[' <form> ',' <form> ')'
       | <unop> <form>
       | <id> <oper> (<id> | <const>)
       | '(' <form> ')'
       | <quantifier> <id> '.' <form>

    <param>  ::= <id> | <const>
    <const>  ::= <string> | <integer>
    <binop>  ::= '->' | '|' | '&' | 'S' [<time>] | Z <timeLE>
    <unop>   ::= '!' |  '@' | 'P' [<time>] | 'H' [<time>] 
    <oper>   ::= '<' | '<=' | '=' | '>' | '>='    
    <quantifier> ::= 'exists' | 'forall' | 'Exists' | 'Forall'
    <time>   ::= <timeLE> | <timeGT>
    <timeLE> ::= '[<=' <number> ']'
    <timeGT> ::= '[>' <number> ']'    

### Event, Macro, and Property Definitions

A specification document ``<doc>``consists of a sequence of definitions. A definition ``<def>``
can either be an event definition, a macro definition, or a property to be checked.

An event definition introduces those events that the property will refer to. If no events are defined then the events are inferred to be those referred to in the property. It may help to reduce errors in properties to declare the events up front.

A predicate macro definition ``<macrodef>`` introduces a named shorthand for a formula, possibly
parameterized with variable names (those introduced with quantifiers). For example the following
macro definition defines a shorthand representing that a file is open:

    pred isOpen(f) = !close(f) S open(f)

Macros can be called in properties. Macros can call other macros, but cannot be recursive. The order of the declaration is of no importance. They can furthermore be introduced after as well as before the properties referring to them.

A property definition ``<propdef>`` introduces a named property, which is a first-order past time temporal formula. 

### Formulas

The different formulas ``<form>`` have the following intuitive meaning:

    true, false     : Boolean truth and falsehood 
    id(v1,...,vn)   : event or call of predicate macro, where vi can be a constant or variable
    p -> q          : p implies q
    p | q           : p or q
    p & q           : p and q
    p S q           : p since q (q was true in the past, and since then, including that point in time, p has been true) 
    p S[<=d] q      : p since q but where q occurred within d time units
    p S[>d] q       : p since q but where q occurred earlier than d time units
    p Z[<=d] q      : p since q but where q did not occur at the current time
    [p,q)           : interval notation equivalent to: !q S p. This form may be easier to read.
    ! p             : not p
    @ p             : in previous state p is true
    P p             : in some previous state p is true
    P[<=d] p        : in some previous state within d time units p is true
    P[>d] p         : in some previous state earlier than d time units p is true
    H p             : in all previous states p is true
    H[<=d] p        : in all previous states within d time units p is true
    H[>d] p         : in all previous states earlier than d time units p is true
    x op k          : x is related to variable or constant k via op. E.g.: x < 10, x <= y, x = y, x >= 10, x > z   
    // -- quantification over seen values in the past, see (*) below:
    exists x . p(x) : there exists an x such that seen(x) and p(x) 
    forall x . p(x) : for all x, if seen(x) then p(x)
    // -- quantification over the infinite domain of all values:
    Exists x . p(x) : there exists an x such that p(x) 
    Forall x . p(x) : for all x p(x)    

    (*) seen(x) holds if x has been observed in the past

### Rules

A new extension of DejaVu is the notion of _rules_, which are part of a property definition. As an example, consider the following property about threads being spawned in an operating system. We want to 
ensure that when a thread `y` reports some data `d` 
back to another thread `x`, then thread `y` has been spawned by thread `x` either directly, or transitively 
via a sequence of spawn events. The events are
`spawn(x,y)` (thread `x` spawns thread `y`)
and `report(y,x,d)` (thread `y` reports data `d`
back to thread `x`). For this we need to compute a transitive closure of spawning relationships, here expressed with the rule `spawning(x,y)`. This property can be stated as follows using the rule `spawned(x,y)` (thread `x` spawned `y`, either directly or indirectly through other spawns):

    prop spawning :
      Forall x . Forall y . Forall d . report(y,x,d) -> spawned(x,y) 
      where 
        spawned(x,y) := 
            @ spawned(x,y) 
          | spawn(x,y) 
          | Exists z . (@spawned(x,z) & spawn(z,y))

The property states that if there is a `report(y,x,d)` event (thread `y` reporting data `d` back to thread `x`), then `spawned(x,y)` must hold, defined as follows: either `spawned(x,y)` held in the previous state, or there is a `spawn(x,y)` in the current state, or, the interesting case: `spawned(x,z)` held in the previous state for some `z`, and  `spawn(z,y)` holds in the current state. This last disjunct forms the transitive closure.

### Time

Timing properties can be expressed using natural numbers as constraints. Examples formulas concerning commands being
dispatched `dis(m)` and succeeding `suc(m)` are:

    Forall m . suc(m) -> true S[<=3] dis(m) // succeeding command must have been dispatched within 3 time units 
    Forall m . suc(m) -> true S[>3] dis(m) // dispatch earlier than 3 time units
    Forall m . dis(m) -> ! (true Z[<=3] dis(m)) // if command dispatched, not dispatched before! within 3
    Forall m . suc(m) -> P[<=3] dis(m) // succeeding command must have been dispatched within 3 time units 
    Forall m . suc(m) -> P[>3] dis(m) // dispatch earlier than 3 time units
    Forall m . suc(m) -> H[<=3] !dis(m) // for a succeeding command no dispatch within 3 time units before
    Forall m . suc(m) -> H[>3] !dis(m) // no dispatch earlier than 3 time units

## Further Examples Of DejaVu Properties

### Auctions

We illustrate the logic with properties that an auction has to satisfy. The following
observable events occur during an auction:

* `list(i,r)` : item `i` is listed for auction with the minimal reserve sales price `r`.
* `bid(i,a)`  : the bidding of `a` dollars on item `i`. 
* `sell(i)`   : the selling of item `i` to highest bidder. 

An auction system has to satisfy the four properties shown in below
expressed over these three kinds of events, using a predicate macro ``inAuction(x)`` to
express when an item ``x`` is in active auction (when it has been listed and not yet sold):

    pred inAuction(x) = exists r . @ [list(x,r),sell(x))

    prop incr : 
      Forall i . Forall a1 . Forall a2 . @ P bid(i,a1) & bid(i,a2) -> a1 < a2

    prop sell : 
      Forall i . Forall r . P list(i,r) & sell(i) ->  exists a . P bid(i,a) & a >= r
  
    prop open : 
      Forall i . Forall a . (bid(i,a) | sell(i)) -> inAuction(i)
    
    prop once : 
      Forall i . Forall r . list(i,r) -> ! exists s . @ P list(i,s)
    
Property `incr` states that bidding must be increasing.

Property `sell` states that when an item is sold, there must exist a bidding
on that item which is bigger than or equal to the reserve price.

Property `open` states that bidding on and selling of an item are
only allowed if the item has been listed and not yet sold.

Finally, Property `once` states that an item can only be listed
once.

### Locks in a multithreaded system

We observe the following events:

* `acq(t,l)`   : thread `t` acquires lock `l`. 
* `rel(t,l)`   : thread `t` releases lock `l`. 
* `read(t,x)`  : thread `t` reads variable `x`.
* `write(t,x)` : thread `t` writes variable `x`.
* `sleep(t)`   : thread `t` goes to sleep.

#### Basic properties

The first set of properties are stated as a conjucntion of three sub-properties:

* A thread going to sleep should not hold any locks.
* At most one thread can acquire a lock at a time.
* A thread can only release a lock it has acquired.

This is formalized as follows:

    prop locksBasic :
      Forall t . Forall l .
        (
          (sleep(t) -> ![acq(t,l),rel(t,l))) &
          (acq(t,l) -> ! exists s . @ [acq(s,l),rel(s,l))) &
          (rel(t,l) -> @ [acq(t,l),rel(t,l)))
        )

#### No deadlocks

Locks should not be acquired in a cyclic manner amongst threads (dining philosopher problem). That is, if a thread `t1` takes a lock `l1` and then a lock `l2` (without having released `l1`), then at no time should another thread `t2` take the locks in reverse order. Obeying this principle will prevent cyclic deadlocks.

This is formalized as follows:

    prop locksDeadlocks :
      Forall t1 . Forall t2 . Forall l1 . Forall l2 .
        (@ [acq(t1,l1),rel(t1,l1)) & acq(t1,l2))
        ->
        (! @ P (@ [acq(t2,l2),rel(t2,l2)) & acq(t2,l1)))

#### No dataraces

If two threads access (read or write) the same shared variable, and one of the threads write to the variable, there must exist a lock, which both threads hold whenever they access the variable.

This is formalized as follows:

    prop locksDataraces :
      Forall t1 . Forall t2 . Forall x .
        (
          (P (read(t1,x) | write(t1,x)))
          &
          (P write(t2,x))
        )
        ->
        Exists l .
          (
            H ((read(t1,x) | write(t1,x)) -> [acq(t1,l),rel(t1,l)))
            &
            H ((read(t2,x) | write(t2,x)) -> [acq(t2,l),rel(t2,l)))
          )

### Calling Earth

This property concerns a radio on board a spacecraft, which communicates over different channels (quantified over in the formula), which each can be turned on and off with a `toggle(x)` - they are all initially off. Telemetry can only be sent to ground over a channel `x`, with the `telem(x)` event, when radio channel `x` is toggled on. The property cannot be expressed in pure past time LTL, that is, without the use of rules.

The property is formalized as follows:

    prop telemetry1: 
      Forall x . closed(x) -> !telem(x) 
      where closed(x) := toggle(x) <-> @!closed(x) 

The same property can alternatively be expressed using two rules, more closely reflecting how we would model this using a state machine with two states for each channel `x`: `closed(x)` and `open(x)`: 

    prop telemetry2: 
      Forall x . closed(x) -> !telem(x) 
        where
        closed(x) := 
            (!@true & !toggle(x)) 
          | (@closed(x) & !toggle(x)) 
          | (@open(x) & toggle(x)),
        open(x) := 
            (@open(x) & !toggle(x)) 
          | (@closed(x) & toggle(x))

The rule `closed(x)` is defined as a disjunction between three alternatives. The first alternative states that this predicate is true if we are in the initial state (the only state where `@true` is false), and there is no `toggle(x)` event. The next alternative states that `closed(x)` was true in the previous state and there is no `toggle(x)` event now. The third alternative states that we in the previous state were in the `open(x)` state and we observe a `toggle(x)` event. Similarly for the `open(x)` rule.

## Enhancing DejaVu for Advanced Prediction

We have extended DejaVu to include the capability of predict the next `k` steps using two distinct 
prediction modes. The fundamental functionality of DejaVu remains intact, with no alterations or 
enhancements. To facilitate prediction requests, we had to adapt the way DejaVu acquires and processes 
its command-line arguments, as outlined [above](#running-dejavu).

Our proposed RV prediction method, termed as ``iPRV``, is predicated on computing an equivalence 
relation on the observed data values. The equivalence classes are independently calculated for each 
variable. Subsequently, we select a representative for the next event from each equivalence class, 
thereby making the following event options a minimum requirement. 
Moreover, for values that have not yet appeared in the trace and belong to the same equivalence class, 
a single representative suffices (after using that representative in the current event, 
a fresh representative is needed for the unseen values, and so on).

### Prediction Modes

We provide two unique prediction modes:

* ``brute`` - This mode employs a simple brute force strategy, exploring all possible options 
without any optimizations for speed or efficiency. It is primarily included for testing, 
experimentation, and comparative purposes, enabling users to contrast it with our 
``iPRV`` (Isomorphic Predictive RV) prediction method.

* ``iPRV`` - This mode forms the crux of our research. The intelligent prediction method capitalizes 
on the fact that some values are equivalent within a specific time frame, thereby diminishing the 
number of potential predictions. In certain scenarios, such as after evaluating a lengthy trace 
file comprising various events, the reduction is so significant that the ``iPRV`` approach 
facilitates successful prediction, while the ``brute`` method fails due to excessive memory usage 
or for reaching to a time limitation.

### Limitations of iPRV-DejaVu
* **Single Event Prediction**: The prediction process is limited to generating only one event per time point.
* **Operator Constraints**: No tests have been conducted for specifications containing operators, such as x < 10, x <= y, or those referring to time units. The prediction procedure currently overlooks these operators.
* **Specification Support**: The tool is designed to support one specification per monitor.



## Modifying the DejaVu Runtime Verification Tool

This part provides a detailed guide on how to modify the DejaVu Runtime Verification (RV) tool. The primary functions of DejaVu are found in three main source files: [Verify.scala](https://github.com/moraneus/iPRV-DejaVu/blob/master/src/main/scala/dejavu/Verify.scala), [Monitor.scala](https://github.com/moraneus/iPRV-DejaVu/blob/master/src/main/scala/dejavu/Monitor.scala), and [Ast.scala](https://github.com/moraneus/iPRV-DejaVu/blob/master/src/main/scala/dejavu/Ast.scala).

1. The `Verify.scala` file manages the creation process of the `TraceMonitor.scala` file. 
It generates this monitor based on the user-provided specifications. Additionally, 
it is responsible for executing the resulting `TraceMonitor.scala`.

2. The `Monitor.scala` file contains common monitoring code used across all properties. 
Importantly, `Monitor.scala` should always be identical to `Monitor.txt` to ensure consistency.

3. The `Ast.scala` file has a crucial role in parsing the spec file provided by the user. 
It generates the appropriate functions to be run on the `TraceMonitor.scala`.

### Guide to Modifying

When `TraceMonitor.scala` is created during the generation process, it uses code templates found in the aforementioned files. 
If you want to modify the final output of `TraceMonitor.scala`, you'll need to make changes in these source files. 
This often involves altering the existing code templates or adding new ones as per your needs.

### Build a New DejaVu.jar Artifact in IntelliJ

After making modifications to the DejaVu Runtime Verification (RV) tool, you may want to create a new `dejavu.jar` artifact. Here are the steps to do this using IntelliJ:

#### Pre-requisites

Before you begin, please ensure you have Scala version 2.11.x installed on your computer. DejaVu was developed on Scala version 2.11.12, so using a different minor version may cause unexpected issues.

#### Steps

1. **Open Project in IntelliJ**: Navigate to your project directory and open the project in IntelliJ.

2. **Setup Scala SDK**: Go to `File -> Project Structure -> Global Libraries`, then add the Scala 2.11.12 SDK.

3. **Create Artifact Configuration**: Go to `File -> Project Structure -> Artifacts`, click on the `+` button, and select `JAR -> From modules with dependencies`.

4. **Select Module and Main Class**: A new window will appear. Select the main module and the main class for the JAR artifact ([Verify.scala](https://github.com/moraneus/iPRV-DejaVu/blob/master/src/main/scala/dejavu/Verify.scala)).

5. **Finalize Artifact Configuration**: IntelliJ will automatically select the output directory for the artifact (it can be modified by you). Click `OK` to save the configuration.

6. **Build Artifact**: Go to `Build -> Build Artifacts`, select `dejavu`, and then select `Build`.

The `dejavu.jar` file will be created in the output directory you specified in the artifact configuration.

#### ⚠️ Important Note on Scala Versions

DejaVu is based on an older version of Scala (2.11.12). If you're using a newer version of Scala, you might encounter compatibility issues. Therefore, it's crucial to ensure you have Scala 2.11.x installed and selected as the SDK in IntelliJ before you proceed with the artifact creation.


## Experiments for publications

- Experiments for FMSD journal paper 2018 "_First-Order Temporal Logic Monitoring with BDDs_": [experiments](https://github.com/havelund/dejavu/tree/master/src/test/scala/tests_fmsd) 

- Experiments for ATVA 2020 paper "_First-Order Timed Runtime Verification using BDDs_": [experiments](https://github.com/havelund/dejavu/tree/master/src/test/scala/tests_atva2020)  

- Experiments for STTT journal 2020 paper "_An Extension of LTL with Rules and its Application to Runtime Verification_":
  * [Wolper's property](https://github.com/havelund/dejavu/tree/master/src/test/scala/tests/test39_formalise_wolper)
  * [Wolper's property as state machine](https://github.com/havelund/dejavu/tree/master/src/test/scala/tests/test41_formalise_statemachine)
  * [task spawning](https://github.com/havelund/dejavu/tree/master/src/test/scala/tests/test42_formalise_taskspawning)

## Contributors - For Original DejaVu
* [Klaus Havelund](http://www.havelund.com), Jet Propulsion Laboratory/NASA, USA
* [Doron Peled](http://u.cs.biu.ac.il/~doronp), Bar Ilan University, Israel
* [Dogan Ulus](https://www.linkedin.com/in/doganulus), Boston University, USA

## Contributors - For iPRV DejaVu
* [Doron Peled](http://u.cs.biu.ac.il/~doronp), Bar Ilan University, Israel
* [Moran Omer](https://github.com/moraneus), Bar Ilan University, Israel