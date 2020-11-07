# Auction
- **Goal** : maximize profit
- Rules :
  - not forced to bid above the marginal cost !
  - do not know how many tasks will be auctionned
  - do know the tasks distribution --> speculate !
  - do know the winner of all tasks.


## To do : CPMaker improvements
- [ ] Add `addTask()` to `CPMaker`. Goal : strategically place the new task in the former plan found, and search from this configuration (hopefully reduces greatly the number of iterations)
- [ ] Add a stop condition to `CPMaker` based on score gradient. E.g stop if 1000 iterations with the same score
- [ ] Add `randomShake(n)` to take `n` random neighbors and shake the current solution. **Avoid computing all the neighbors for this step**


## To do : Bider improvements
- [ ] How much to bid over the marginal cost ?
- [ ] How to use knowledge about opponents's tasks ? 
- [ ] Keep track of opponents' tasks