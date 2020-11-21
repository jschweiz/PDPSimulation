# Auction
- **Goal** : maximize profit
- Rules :
  - not forced to bid above the marginal cost !
  - do not know how many tasks will be auctionned
  - do know the tasks distribution --> speculate !
  - do know the winner of all tasks.
  - **2 vehicles only**
  - **Always 1 vs 1**

## Questions
- How many competitors at max ? --> if not too many, can compute the plan for each competitor

## Biding strategy
- We are in a *reverse auction* mechanism
- No obvious paper talking about this particular situation --> dig deeper


## To do : CPMaker improvements
### Not wasting the previous computations (compute smarter)
Idea : when proposing a task, start computing the optimal plan starting by the previous plan found + the new proposed task. Goal : reduce computing time
- [x] Add possibility to run `CPMaker` starting from a given VariableSet. 
- [x] Implement `~addTask` to `VariableSet` : strategically place the new task in the former plan found, and search from this configuration (hopefully reduces greatly the number of iterations)
- [x] Implement `removeLastTask` : if the plan computed with a new task has a cost < the plan before adding the task, then just remove the new task and update the winningPlan. (Benefit if we do not win the auction)



**Conclusion** : 
- with 20 tasks, 2000 iterations, 2 vehicles : the "compute from scratch" method and the "compute smarter" method yields the same score *in average*
- with 20 tasks, 2000 iterations, 5 vehicles : the "compute smarter" is slightly better (~700$ saved, for a total cost of 16000 $ in average)
- --> **Not great, but still time saving for biding strategy**

### Stop computing when it is useless to continue
- [ ] Add a stop condition to `CPMaker` based on score gradient. E.g stop if 1000 iterations with the same score

### Restart computation based on a randomized solution
- [X] Add `randomShake(VariableSet vs, int numSteps)` to `CPMaker` to take `n` random neighbors and shake the current solution. **Please avoid computing all the neighbors for this step**
- [ ] Implement restart


## To do : Bider improvements
- [x] How much to bid over the marginal cost ?
- [ ] How to use knowledge about opponents's tasks ? 
- [x] Keep track of opponents' tasks and bids

## To do : Testing
- [x] Setup tournament
  
**To launch a tournament**:
- export jar with only the classes of Auction + library (no Centralized, Reactive, ...) and place the `Agents.jar` in `agents/`
- launch `runTournament` (run/debug tab of vscode)

