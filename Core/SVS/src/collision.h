#ifndef COLLISION_H
#define COLLISION_H

#ifdef ENABLE_ROS

#include <ompl/base/StateValidityChecker.h>
#include <fcl/broadphase/broadphase_dynamic_AABB_tree.h>
#include <fcl/collision.h>

#include "robot_model.h"

/*
 * collision_data struct
 *
 * Why doesn't FCL provide this?
 */

struct collision_data {
    collision_data() {
        done = false;
    }

    fcl::CollisionRequest request;
    fcl::CollisionResult result;
    bool done;
};

// Same question as above!
bool collision_function(fcl::CollisionObject* o1,
                        fcl::CollisionObject* o2, void* cdata);

/*
 * object_data struct
 *
 * To pass in as user data for each collision object
 */
struct object_data {
    std::string name;
    std::set<std::string> allowed_collisions;
};

class collision_checker : public ompl::base::StateValidityChecker {
public:
    collision_checker(ompl::base::SpaceInformation* si);
    collision_checker(const ompl::base::SpaceInformationPtr& si);
    collision_checker(ompl::base::SpaceInformation* si,
                      robot_model* m,
                      std::string group);
    collision_checker(const ompl::base::SpaceInformationPtr& si,
                      robot_model* m,
                      std::string group);

    bool isValid(const ompl::base::State* state) const override;

private:
    std::vector<std::string> joint_names;
    std::shared_ptr<robot_model> model;

    fcl::BroadPhaseCollisionManager* robot;
    fcl::BroadPhaseCollisionManager* world;
};

#endif
#endif