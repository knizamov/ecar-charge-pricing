# Choosing high level architecture for the eCar charge pricing system

* Status: accepted
* Deciders: Kirill Nizamov
* Date: 2019-12-10

## Context and Problem Statement
eCar company is going to introduce new way pricing in their new, successful backoffice supporting electric vehicles charging network.     
* Ability to define and calculate a price via API.    
* A price definition will contain price per minute and can be defined for specific duration, e.g. one price before noon and different one afternoon   
* A calculation input will contain start and end datetime of charging process and customer id   
* For VIP customers we will grant 10% discount   
* A solution should be provided in a zip file or as a link to git repository   
* An application should be able to be launched from command line with single command like "java –jar...."   
* A solution should require no additional software to be installed except for JRE 8  

We have to design and choose an architecture for the new eCar charge pricing system.
The scope is limited to the charge pricing domain only.

Here is a [link](https://docs.google.com/document/d/1tG-fLnFY5s2s_8WMBccpL69KHs7lssSmF07RrqgJixI/edit?usp=sharing)
I sent to the customer to some analysis in the form of BDD scenarios (some are probably missing and 
some require validation from the customer) and some questions I wanted to discuss, verify and 
validate with the customer. But I was told to do it based on my own assumptions.

## Decision Drivers

* ambiguity of business requirements, assumptions and vectors of changes 
* testability
* maintainability 

## Considered Options

* [Single Charge Pricing module](#1-single-charge-pricing-module)
* [Prices and Charging Costs modules (Charging Costs knows about the price definition structure)](#2-prices-and-charging-costs-modules-charging-costs-knows-about-the-price-definition-structure)
* [Prices and Charging Costs modules (Charging Costs knows only the total cost/price rate for period)](#3-prices-and-charging-costs-modules-charging-costs-knows-only-the-total-costprice-rate-for-period)

## Pros and Cons of the Options

Before diving into each option, let's setup common assumptions.

The actors of the system are probably going to stay stable (or more stable) across all the architectures:
* **Customer** that has an electric vehicle and needs to charge it and know charging costs
* **Charging Station** that notifies about finished charging sessions
* **Price Manager** that manages raw charging prices for specific periods

The **Customers** Software System is also going to be considered fixed and external,
because there is no need for us to manage any of the customer information (at least for now).
It's needed for the sole purpose of getting the customer status
that is needed for applying discounts, but in the future we could also anticipate
getting this information from some other sources (e.g. JWT).

What unclear and probably unstable is 
the internal relationship and communication between **Prices** and **Charging Costs**
so different architectural options are what we are going to consider next.

### 1. Single Charge Pricing module
![option 1](../diagrams/architecture_option_1.png)
This architecture represents a single module for Charge Pricing
that calculates charging costs with managed price definitions.

Given the nature of our requirements, some of the interactions between **Prices** and **Charge Costs** are unclear
(see [option 2](#2-prices-and-charging-costs-modules-charging-costs-knows-about-the-price-definition-structure) for more details).
That's why I'm a bit reluctant to include those concerns in the high level architecture
setting hard boundaries that we are unsure of. Setting boundaries and splitting the solution into 2 modules
would mean having a contract between them which also means that ideologically we have to mock a module in our tests
(see "Keep IT clean" talk by Jakub Nabrdalik). Also, a module is microservice candidate and I once asked Jakub Nabrdalik 
how to deal with the situation when you have an acceptance test that covers multiple modules
but in the future you might decide to extract a module into its own microservice and it's no longer easy to
run a single acceptance test because now you have 2 applications (probably you would have to spin them up in a docker).
I got an answer from Jakub Nabrdalik that they had moved away from an acceptance test for more than one module - and
this is what I'm going to follow as well.

So, this option is going to hide **Prices** and **Charging Costs** concerns and keep them as an implementation detail
but internally is going to be similar to the [option 2](#2-prices-and-charging-costs-modules-charging-costs-knows-about-the-price-definition-structure) 
or [option 3](#3-prices-and-charging-costs-modules-charging-costs-knows-only-the-total-costprice-rate-for-period).
This will allow us to wait for more requirements/refinements/changes 
and see what direction to take and solidify it in the architecture later.
But of course we have to keep an eye on this architecture not to create a Big Ball of Mud.

* Pros
    * simpler solution
    * have time to mature and not having to set boundaries and contracts too early
    while still maintaining simplicity
    * tests are easier to write compared to multiple modules 

* Cons
    * having one bigger module is more prone to being neglected and turning into a BBOM

### 2. Prices and Charging Costs modules (Charging Costs knows about the price definition structure)
![option 2](../diagrams/architecture_option_2.png)

This architecture splits **Charge Pricing** domain into two modules.
* **Prices** module manages price definitions for charging defined by **Price Manager**
* **Charging Costs** module calculates charging costs
 
In order for **Charging Costs** module to do the calculations, 
it gets prices for specific period of time from **Prices** module. And this is where uncertainty lies.
It seems that now **Charging Costs** module have to know the structure of price definitions and deal with
applying them to periods of time and time intervals which might be OK 
since it may be the essential complexity that we can't get rid of (we don't know yet)
but I'm not sure if promoting it to the level of high level architecture is justified just yet 
(again given the nature of our requirements and concerns expressed in the [option 1](#1-single-charge-pricing-module))

* Pros
    * more formalized and detailed communication and structure with more specified responsibilities
    
* Cons
    * the communication, responsibilities and structure may not be stable and mature enough 
    to promote it to the level of high level architecture 
    given some of the business requirements and assumptions are unstable and ambiguous itself
    * bigger structural changes in application code and tests needed if we get the boundaries wrong

### 3. Prices and Charging Costs modules (Charging Costs knows only the total cost/price rate for period)
![option 3](../diagrams/architecture_option_3.png)

The only difference compared to the [option 2](#2-prices-and-charging-costs-modules-charging-costs-knows-about-the-price-definition-structure) 
is instead of **Charging Costs** knowing price definitions, it asks only for total costs (or total price rate) for the given period. 
This allowed us to get rid of structural coupling on price definitions
by moving raw total charging cost (or total price rate) calculations (without applying any other rules such as discounts)
to **Prices** module. And now **Charging Costs** module has to apply this total cost (or total price rate) 
and deal with other business rules (such as discounts) and charging sessions.
But it lost the information about calculations for specific periods of times and intervals which might be needed in the future
(we don't know yet). We might try to mitigate it by having **Prices** modules return details of calculations 
but I'm now sure how feasible that is.

* Pros
    * all the pros of [option 2](#2-prices-and-charging-costs-modules-charging-costs-knows-about-the-price-definition-structure)
    * limits structural coupling on price definition

* Cons
    * all the cons of [option 2](#2-prices-and-charging-costs-modules-charging-costs-knows-about-the-price-definition-structure)
    * the scope of communication with **Prices** may be too limiting
    
## Decision Outcome
Given aforementioned options and their pros and cons, we are going to choose 
[Single Charge Pricing module](#1-single-charge-pricing-module) as a counteract to somewhat ambiguous
future business requirements and assumptions (or maybe I'm just missing something and my reasoning is totally flawed in which case I'm sorry).
This option seems the most simplistic while allowing to more easily adopt to future changes without
committing to hard boundaries on things that are likely to change or that are uncertain (especially given the nature of the new system).

## Links
* [BDD scenarios and questions discussion](https://docs.google.com/document/d/1tG-fLnFY5s2s_8WMBccpL69KHs7lssSmF07RrqgJixI/edit?usp=sharing)