package edu.mit.mitmobile2.dining;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import edu.mit.mitmobile2.DividerView;
import edu.mit.mitmobile2.R;
import edu.mit.mitmobile2.dining.DiningMealIterator.MealOrEmptyDay;
import edu.mit.mitmobile2.dining.DiningModel.DiningDietaryFlag;
import edu.mit.mitmobile2.dining.DiningModel.HouseDiningHall;
import edu.mit.mitmobile2.dining.DiningModel.Meal;
import edu.mit.mitmobile2.dining.DiningModel.MenuItem;

public class DiningHouseScheduleSliderAdapter extends DiningHouseAbstractSliderAdapter {
	
	Context mContext;
	
	private String mHallID;
	
	public DiningHouseScheduleSliderAdapter(Context context, HouseDiningHall hall, long selectedTime) {
		super(context);
		
		mContext = context;
		mHallID = hall.getID();
		
		GregorianCalendar day = new GregorianCalendar();
		day.setTimeInMillis(selectedTime);
		ArrayList<HouseDiningHall> halls = new ArrayList<HouseDiningHall>();
		halls.add(hall);
		DiningMealIterator mealIterator = new DiningMealIterator(day, halls);
		setMealIterator(mealIterator);
	}

	@Override
	protected View viewForMealOrDay(MealOrEmptyDay mealOrEmptyDay) {
		if (mealOrEmptyDay.isEmpty()) {
			return noMealsTodayScreen(mealOrEmptyDay.getDayMessage());
		} else {
			return mealScreen(mealOrEmptyDay.getMeal(mHallID));
		}		
	}
	
	private View noMealsTodayScreen(String message) {
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dining_meal_message, null);
		TextView messageView = (TextView) view.findViewById(R.id.diningMealMessageText);
		messageView.setText(message);
		return view;
	}
	
	private View mealScreen(Meal meal) {
		
		// Parent Layout
		ScrollView scrollWrapper = new ScrollView(mContext);
		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollWrapper.addView(layout);
		
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		List <DiningDietaryFlag> appliedFilters = DiningDietaryFlag.loadFilters(mContext);
		boolean noSelectedFilters = appliedFilters.isEmpty();
		// filter header
		float density = mContext.getResources().getDisplayMetrics().density;
		int iconSquare = (int)Math.ceil(18 * density);
		int iconMargin = (int)Math.ceil(7 * density);
		Log.d("MARGIN", iconMargin + "");
		if (!appliedFilters.isEmpty()) {
			View filterHeader = inflater.inflate(R.layout.dining_filter_header, null);
			LinearLayout iconContainer = (LinearLayout)filterHeader.findViewById(R.id.filterIdContainer);
			LayoutParams params = new LayoutParams(iconSquare, iconSquare);
			params.setMargins(iconMargin, 0, 0, 0);
			for (DiningDietaryFlag flag : appliedFilters) {
				ImageView iconView = new ImageView(mContext);
				iconView.setLayoutParams(params);
				iconView.setBackgroundResource(flag.getIconId());
				iconContainer.addView(iconView);
			}
			layout.addView(filterHeader);
		}
		
		// Meal header
		
		View mealHeader = inflater.inflate(R.layout.dining_meal_header, null);	
		TextView mealTitleView = (TextView) mealHeader.findViewById(R.id.diningMealHeaderTitle);
		TextView mealTimeView = (TextView) mealHeader.findViewById(R.id.diningMealHeaderTime);
		mealTitleView.setText(meal.getCapitalizedName());
		
		if (meal.getScheduleSummary() != null) {
			mealTimeView.setText(meal.getScheduleSummary());
		}
		layout.addView(mealHeader);
		
		
		// meal message or list of meal items
		if (meal.getMessage() != null) {
			View view = inflater.inflate(R.layout.dining_meal_message, null);
			TextView messageView = (TextView) view.findViewById(R.id.diningMealMessageText);
			messageView.setText(meal.getMessage());			
		} else {
			
			if (appliedFilters.isEmpty()) {
				// no filters applied means all filters are applied
				appliedFilters = new ArrayList<DiningDietaryFlag>(DiningDietaryFlag.allFlags());
			}
			
			boolean showingItems = false;
			for (MenuItem menuItem : meal.getMenuItems()) {
				
				boolean showItem = false;
				if (menuItem.getDietaryFlags().isEmpty() && noSelectedFilters) {
					// if menuItem does not have any flags and no filters have been selected (before we take all filters)
					showItem = true;
				}
				for (DiningDietaryFlag menuFlag : menuItem.getDietaryFlags()) {
					if (appliedFilters.contains(menuFlag)) {
						showItem = true;
						break;
					}
				}
				
				if (showItem) {
					showingItems = true;
					View view = inflater.inflate(R.layout.dining_meal_item_row, null);
					TextView stationView = (TextView) view.findViewById(R.id.diningMealItemRowStation);
					TextView nameView = (TextView) view.findViewById(R.id.diningMealItemRowName);
					TextView descriptionView = (TextView) view.findViewById(R.id.diningMealItemRowDescription);
					TableLayout dietaryFlags = (TableLayout) view.findViewById(R.id.diningMealItemRowDietaryFlagsTable);
					
					stationView.setText(menuItem.getStation());
					nameView.setText(menuItem.getName());
					if (menuItem.getDescription() != null) {
						descriptionView.setText(menuItem.getDescription());
					} else {
						descriptionView.setVisibility(View.GONE);
					}
					
					List<DiningDietaryFlag> flags = menuItem.getDietaryFlags();
					TableRow tableRow = null;
					int columns = 2; 
					for (int i = 0; i < flags.size(); i++) {
						if (i % columns == 0) {
							// lets make new row
							tableRow = new TableRow(mContext);
							tableRow.setGravity(Gravity.RIGHT);
							dietaryFlags.addView(tableRow);
						}
						ImageView flagImageView = new ImageView(mContext);
						DiningDietaryFlag flag = flags.get(i);
						flagImageView.setImageResource(flag.getIconId());
						tableRow.addView(flagImageView);
					}
					
					layout.addView(view);
					layout.addView(new DividerView(mContext, null));
				}
			}
			
			if (!showingItems) {
				layout.addView(getEmptyMenuView("No matching items"));
			}
		}
		
		return scrollWrapper;
	}
	
	private View getEmptyMenuView(String message) {
		TextView emptyMessage = new TextView(mContext);
		emptyMessage.setGravity(Gravity.CENTER_HORIZONTAL);
		emptyMessage.setTextAppearance(mContext, R.style.ListItemPrimary);
		emptyMessage.setText(message);
		int topPadding = mContext.getResources().getDimensionPixelSize(R.dimen.standardPadding);
		emptyMessage.setPadding(0, topPadding, 0, 0);
		return emptyMessage;
	}
	
	
}