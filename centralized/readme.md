# TO DO
- [x] Correct construction of initial solution
  - [x] Avoid forgetting task 
  - [x] Avoid raising unfounded exception
- [x] Write `getCost()` / try an efficient way to calculate this
- [x] Write choice function `localChoice()` to choose best transformation with probabilty p
- [x] Write `conditionIsMet()`
- [x] Implement equals and hashcode for VariableSet (use of HashSet in `chooseNeighbours()`)
- [ ] Correct `computeNumberOfTaskVehicle()` --> yields +1 to the result

## 01/11/20 : testing
- Easily stuck in local minima. E.g : put costPerKm of vehicle 0 to 1000/km and see. When the number of task initially assigned to it > 7, it is stuck in a local minima where the vehicle 0 still has to do things.
- Solutions
  - [x] To neighbors, add changing of vehicles for not only first task (limited effect)
  - [x] Possibility to change several tasks in one step (seems to work ! No improvement with first solution)
  - [ ] Randomize initial solution (perfom change vehicles arbitrarily initially)