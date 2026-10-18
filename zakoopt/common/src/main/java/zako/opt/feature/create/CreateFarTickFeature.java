package zako.opt.feature.create;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "create_far_tick",
	group = FeatureGroup.CREATE,
	mixins = "block.CreateFarTickMixin"
)
public class CreateFarTickFeature extends Feature {
}
