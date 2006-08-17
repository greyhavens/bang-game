class Card < ActiveRecord::Base
  include ActionView::Helpers::AssetTagHelper

  def self.find_all_for_town (town)
    find(:all, :conditions => [ "town = ?", town ], :order => "name ASC")
  end

  def done?
    self.implemented? &&
      (bonus_model == "" || bonus_model_path != "") &&
      (activation_viz == "" || activation_viz_path != "") &&
      (activation_sound == "" || activation_sound_path != "") &&
      (ongoing_viz == "" || ongoing_viz_path != "") &&
      (special_sound == "" || special_sound_path != "");
  end
end
